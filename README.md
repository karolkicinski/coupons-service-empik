# Coupons Service

## Architektura

Aplikację podzieliłem warstwowo, aby skutecznie oddzielić odpowiedzialności poszczególnych elementów rozwiązania.

Główna logika biznesowa znajduje się w warstwie serwisowej.
Kontrolery odpowiadają wyłącznie za obsługę requestów HTTP, walidację wejścia oraz zwracanie odpowiednich odpowiedzi API. 
Dostęp do danych został odseparowany w repozytoriach, a mapowanie między encjami bazodanowymi i obiektami API zostało przeniesione do mapperów.
(dzięki temu encje nie są bezpośrednio eksponowane na zewnątrz aplikacji).

Integracja z zewnętrznym providerem geolokalizacji została wydzielona do osobnego modułu "integration.geolocalisation".
Pozwala to traktować zewnętrzne API jako osobną zależność.

Poniżej podział:

- config: pliki konfiguracyjne cache oraz swaggera,
- controller: kontroler API oraz controllerAdvice do obsługi globalnej błędów,
- enumerate: moduł dla enumów,
- exception: definicje błędów,
- integration: moduł dla wszystkich integracji (w tym przypadku jedna dotycząca odczytu kraju po adresie IP),
- mapper: wszystkie mappery w projekcie,
- model: wszystkie klasy modelu. Tutaj podział na encje bazodanowe oraz DTO: request oraz response,
- repository: interfejsy bazodanowe,
- service: główna logika aplikacji (w tym przypadku jedna: CouponService),
- utils: wszelkiego rodzaju helpery, które mogą być globalne/wykorzystane w innych projektach,
- tests: testy jednostkowe, integracyjne, współbieżności, kontraktowe.

## Technologie + wyjaśnienie

Wykorzystałem następujące technologie:

- Java 17,
- Spring Boot,
- Spring Web MVC,
- Spring Data JPA,
- H2 Database -> baza danych,
- OpenFeign -> komunikacja z zewnętrznym serwisem geolokalizacji,
- Springdoc OpenAPI / Swagger -> dokumentacja API,
- Hazelcast -> cache,
- MapStruct -> mappery,
- JUnit 5 -> testy jednostkowe,
- Mockito -> testy jednostkowe, integracyjne,
- MockMvc -> testy integracyjne,
- WireMock -> testy kontraktowe,
- Spotless -> automatyczne formatowanie kodu, wykorzystałem konwencję googleJavaFormat,
- Liquibase -> zarządzanie schematem bazy danych i migracjami,
- Spring Boot Actuator -> podstawowe endpointy techniczne do monitorowania stanu aplikacji, metryk i diagnostyki.



## Funkcjonalności


Zgodnie z przesłaną treścią zadania zostały dodane (między innymi) następujące funkcjonalności:

1. Tworzenie kuponów z:
    - unikalnym kodem,
    - datą utworzenia,
    - maksymalną liczbą użyć,
    - bieżącą liczbą użyć,
    - kodem kraju, w którym kupon może zostać użyty.
   
2. Kupon jest unikalny - jest to sprawdzane w momencie dodawania nowego kuponu (walidacja), następnie kupon jest
mapowany za pomocą wygenerowanego mappera (MapStruct) do encji oraz jest wykonywana próba zapisu. Tutaj dodatkowe
zabezpieczenie w bazie danych w postaci unique constrainta na kolumnie "code". Dodatkowo dodałem logikę, która 
przy zmianie liter na upperCase konwertuje je neutralnie językowo.

3. "Wykorzystanie kuponu powinno być limitowane maksymalną liczbą użyć – kto pierwszy ten lepszy." - limit użyć kuponu 
nie jest zabezpieczony wyłącznie przez @Transactional (to zapewnia atomowość całej operacji), natomiast główna ochrona 
przed nadpisaniem (i nie tylko) opiera się na atomowej aktualizacji w bazie danych (czyli dodane @Query w CouponRepository).
To baza danych pełni rolę źródła prawdy i nawet przy wielu równoległych requestach/instancjach serwisu
licznik użyć nie przekroczy wartości max_usages. 

4. "Kraj zdefiniowany w kuponie ogranicza użycie kuponu tylko do osób z danego kraju (na podstawie adresu IP – 
można wykorzystać dowolną darmową usługę do tego)." - tutaj wykorzystałem serwis "http://ip-api.com". Dodatkowo,
wykorzystałem cache Hazelcast aby ograniczyć liczbę requestów, tj. jeśli w cache znajduje się już IP (obsłużyliśmy
takie IP w ostatnim czasie i znamy państwo) to nie musimy ponownie wysyłać zapytania do zewnętrznego serwisu. 
Można również założyć dla takiej funkcjonalności dodatkową tabelę w bazie danych i to tam przetrzymywać adresy,
które odpytywały nasz serwis wraz z państwem - dobry pomysł na rozwój. Dodatkowo został wykorzystany interfejs,
więc dostawca może zostać łatwo zmieniony. Do komunikacji z zewnętrznym serwisem wykorzystałem nowoczesne podejście
FeignClient.

5. "Gdy kupon osiągnął maksymalną liczbę zużyć, próby użycia go powinny zwracać stosowną informację
w zwrotce. Tak samo, gdy podany kod kuponu nie istnieje, próba zużycia przychodzi z
niedozwolonego kraju lub użytkownik zużył już dany kupon." - dodałem odpowiednie definicję błędów oraz wiadomości
w enumie CouponRejectionReason oraz obsługę globalnych błędów w ApiExceptionHandler. Walidacja poszczególnych
pól w requeście również jest zwracana.

6. "(Opcjonalnie, dla chętnych) Jeden użytkownik może wykorzystać kupon tylko raz – request powinien
zawierać identyfikator użytkownika (dowolny) oraz kod kuponu do wykorzystania." - dodałem tabelę, która ma w sobie
użycia kuponów. Dodałem tutaj unique contraint na userid oraz coupon code - dodatkowe zabezpieczenie na bazie danych,
aby jeden użytkownik nie wykorzystał ponownie tego samego kuponu. Oczywiście jest to dodatkowo walidowane w samym
serwisie.

7. "Rozwiązanie powinno być skalowalne." - rozwiązanie jest skalowalne. Możemy uruchomić kilka instancji serwisu
na jednej bazie danych. Tak jak wspominałem to baza danych jest źródłem prawdy i to tam następuje atomowa inkrementacja.
Tutaj może pojawić się pytanie - co się stanie jeśli dwie instancje serwisu będą chciały zapisać encję CouponUsage 
a następnie inkrementować wskaźnik użyć. Tutaj zadziałają dwa zabezpieczenia. W transakcji być może uda się zapisać
encje ale tylko jeden z serwisów będzie w stanie inkrementować wskaźnik - dla drugiego zostanie zwrócony 
updateRows = 0, a wiec transakcja sie wycofa - kilka zabezpieczeń! Dodatkowo dzięki sygnaturze UniqueConstraint
(najczęściej) mamy zapewnione po stronie bazy danych indeksy, więc zapewnia to wydajne wyszukiwanie.

8. Dodatkowo dodałem do projektu również definicje swaggerowe oraz endpoint w celu przetestowania oraz 
podejrzenia dokumentacji (dostępny na: http://localhost:8080/swagger-ui/index.html).

   

## Najciekawsze testy

Testy zostały podzielone na kilka poziomów, aby pokryć zarówno logikę jednostkową, jak i pełne scenariusze 
biznesowe oraz przypadki współbieżne.
Oprócz standardowego testu logiki dodałem również testy utils, mapperów oraz metod dotyczących komunikacji z providerem
lokalizacji.

### Testy jednostkowe

- `CouponServiceTests`
- `CouponNormalizerTests`
- `HttpUtilsTests`
- `CouponMapperTests`
- `CouponUsageMapperTests`
- `IpApiGeolocalisationResolverTests`
- `IpApiGeolocalisationResolverCacheTests`

### Testy integracyjne

Tutaj starałem się również dodać testy integracyjne rozwiązania, czyli testy całego kontrolera.

- `CouponControllerIntegrationTests`

### Testy współbieżności

Tutaj chyba najciekawszy przypadek. Bardzo często w zadaniu było zaznaczane, że liczy się kto pierwszy ten lepszy
jeśli chodzi o użycie kuponu. Dodałem test, który sprawdza, czy na pewno kupon zostanie użyty max tyle razy, 
ile zostało to zdefiniowane. Wykorzystałem tutaj klsyczną wielowątkowość opratą na Thread - startowaną w jednym momencie 
dzięki CountDownLatch oraz zbieranie statusów.

- `CouponServiceConcurrencyTests`

### Test kontraktowy

Dodałem dodatkowo, aby zapewnić zgodność kontraktu zewnętrznego serwisu. WireMock symuluje zewnętrzny serwis HTTP, 
dzięki czemu test sprawdza kontrakt klienta Feign bez zależności od prawdziwego API.

- `IpApiGeolocalisationFeignClientContractTests`


## Przykłady użycia (więcej na Swagger UI):

=====> Get all coupons
```http
POST /api/coupons
Content-Type: application/json
```

200:
```json
[
{
"id": 0,
"code": "string",
"createdAt": "2026-05-29T20:49:37.505Z",
"maxUsages": 0,
"currentUsages": 0,
"countryCode": "string"
}
]
```

=====> Create coupon
```http
POST /api/coupons
Content-Type: application/json
```

body:
```json
{
"code": "string",
"maxUsages": 1,
"countryCode": "PL"
}
```

201:
```json
{
"id": 0,
"code": "string",
"createdAt": "2026-05-29T20:50:46.157Z",
"maxUsages": 0,
"currentUsages": 0,
"countryCode": "string"
}
```

=====> Apply coupon
```http
POST /api/coupons/apply
Content-Type: application/json
```

body:
```json
{
"code": "string",
"userId": 0
}
```

201:
```json
{
"success": true,
"code": "string",
"message": "string"
}
```

=====> Get all usages
```http
GET /api/coupons/usages
Content-Type: application/json
```

200:
```json
[
{
"id": 0,
"couponId": 0,
"couponCode": "string",
"userId": 0,
"usedAt": "2026-05-29T20:51:57.678Z",
"ipAddress": "string",
"countryCode": "string"
}
]
```

## Statusy błędów HTTP

| Scenariusz | Status | Kod |
|---|---|---|
| Błąd walidacji requestu | `400 Bad Request` | VALIDATION_ERROR |
| Kupon nie istnieje | `404 Not Found` | COUPON_NOT_FOUND |
| Kraj jest niedozwolony | `403 Forbidden` | COUNTRY_NOT_ALLOWED |
| Kupon już istnieje | `409 Conflict` | COUPON_ALREADY_EXISTS |
| Użytkownik już użył kuponu | `409 Conflict` | USER_ALREADY_USED_COUPON |
| Limit użyć został osiągnięty | `409 Conflict` | COUPON_USAGE_LIMIT_REACHED |
| Błąd providera geolokalizacji | `502 Bad Gateway` | COUNTRY_RESOLUTION_FAILED |


## Uruchomienie projektu

Wymagania:

- Java 17
- Maven

Uruchomienie aplikacji:

bash ./mvnw spring-boot:run

lub 

docker compose up --build
