# Architecture Overview - Refactored Code

## Class Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Controller Layer                         │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
                ┌──────────────────────────────┐
                │   ProfessionalController     │
                │  (@RestController)           │
                │                              │
                │  + getAllProfessionals()     │
                │  + searchProfessionals()     │
                │  + getProfessionalById()     │
                │  + createProfessional()      │
                │  + updateProfessional()      │
                │  + deleteProfessional()      │
                │  + getDistinctCities()       │
                └──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      Service Layer                           │
└─────────────────────────────────────────────────────────────┘
                               │
              ┌────────────────┴────────────────┐
              ▼                                  ▼
┌──────────────────────────┐      ┌──────────────────────────┐
│   ProfessionalService    │      │ ProfessionalSearchService│
│   (@Service)             │      │   (@Service)             │
│                          │      │                          │
│ - professionalRepository │      │ - professionalRepository │
│ - professionalMapper     │      │                          │
│ - searchService          │      │ + search(criteria)       │
│                          │      │ - matchesFilters()       │
│ + getAllProfessionals()  │      │ - matchesQuery()         │
│ + getProfessionalById()  │      │ - matchesCity()          │
│ + searchProfessionals()  │◄─────│ - matchesState()         │
│ + createProfessional()   │      │ - matchesCountry()       │
│ + updateProfessional()   │      │ - matchesRemote()        │
│ + deleteProfessional()   │      │ - matchesAvailability()  │
│ + getDistinctCities()    │      └──────────────────────────┘
└──────────────────────────┘
              │
              ▼
┌──────────────────────────┐
│   ProfessionalMapper     │
│   (@Component)           │
│                          │
│ - skillRepository        │
│                          │
│ + toDTO(entity)          │
│ + toEntity(dto)          │
│ + updateEntityFromDTO()  │
│ - toLocationDTO()        │
│ - toSkillDTO()           │
│ - toServiceDTO()         │
│ - toSocialLinkDTO()      │
│ - updateLocationFromDTO()│
│ - updateSkillsFromDTO()  │
│ - updateServicesFromDTO()│
│ - updateSocialLinksFromDTO()│
└──────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                     Repository Layer                         │
└─────────────────────────────────────────────────────────────┘
              │
              ▼
┌──────────────────────────┐      ┌──────────────────────────┐
│ ProfessionalRepository   │      │   SkillRepository        │
│ (@Repository)            │      │   (@Repository)          │
│                          │      │                          │
│ + findAll()              │      │ + findByName()           │
│ + findById()             │      │ + save()                 │
│ + save()                 │      └──────────────────────────┘
│ + deleteById()           │
│ + searchProfessionals()  │
│ + findBySkillsNameIn()   │
│ + findByCategoryOrSkillsCategory()│
│ + findDistinctCities()   │
└──────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                         DTO Layer                            │
└─────────────────────────────────────────────────────────────┘
              │
    ┌─────────┴──────────────────────┐
    ▼                                 ▼
┌──────────────────────┐   ┌───────────────────────────┐
│  ProfessionalDTO     │   │ ProfessionalSearchCriteria│
│                      │   │   (@Builder)              │
│ - id                 │   │                           │
│ - firstName          │   │ - query                   │
│ - lastName           │   │ - city                    │
│ - headline           │   │ - state                   │
│ - bio                │   │ - country                 │
│ - location           │   │ - remote                  │
│ - skills             │   │ - available               │
│ - services           │   │ - skills                  │
│ - socialLinks        │   │ - categories              │
│ - ...                │   │                           │
└──────────────────────┘   │ + hasAnyFilter()          │
                           │ + hasSkillsFilter()       │
    ┌──────────────────────┤ + hasCategoriesFilter()   │
    ▼                      │ + hasLocationOrAvailabilityFilter()│
┌──────────┐               └───────────────────────────┘
│ SkillDTO │
│ ServiceDTO│
│ SocialLinkDTO│
└──────────┘

┌─────────────────────────────────────────────────────────────┐
│                      Entity Layer                            │
└─────────────────────────────────────────────────────────────┘
              │
    ┌─────────┴──────────────────────┐
    ▼                                 ▼
┌──────────────────┐         ┌──────────────────┐
│  Professional    │         │   Skill          │
│  (@Entity)       │◄────────┤   (@Entity)      │
│                  │ M    N  │                  │
│ - id             │         │ - id             │
│ - firstName      │         │ - name           │
│ - lastName       │         │ - category       │
│ - headline       │         └──────────────────┘
│ - bio            │
│ - city           │         ┌──────────────────┐
│ - state          │         │ ServiceOffering  │
│ - country        │         │   (@Entity)      │
│ - remote         │◄────────┤                  │
│ - isAvailable    │ 1    N  │ - id             │
│ - skills         │         │ - title          │
│ - services       │         │ - description    │
│ - socialLinks    │         │ - priceMin       │
└──────────────────┘         │ - priceMax       │
         │                   └──────────────────┘
         │ 1
         │                   ┌──────────────────┐
         │ N                 │  SocialLink      │
         └───────────────────┤   (@Entity)      │
                             │                  │
                             │ - id             │
                             │ - platform       │
                             │ - url            │
                             │ - label          │
                             └──────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    Exception Layer                           │
└─────────────────────────────────────────────────────────────┘
              │
              ▼
┌──────────────────────────┐
│ ResourceNotFoundException│
│ (extends RuntimeException)│
│                          │
│ + professionalNotFound() │
└──────────────────────────┘
```

## Request Flow

### Example: Search Professionals

```
1. HTTP Request
   ↓
   GET /api/professionals?q=plumbing&city=New York
   ↓

2. Controller Layer
   ↓
   ProfessionalController.getAllProfessionals()
   - Receives query parameters
   - Validates parameters exist
   ↓

3. Service Layer
   ↓
   ProfessionalService.searchProfessionals()
   - Creates ProfessionalSearchCriteria
   - Calls ProfessionalSearchService.search()
   ↓
   ProfessionalSearchService.search()
   - Determines search strategy (skills/category/general)
   - Calls appropriate repository method
   - Applies additional filters if needed
   ↓

4. Repository Layer
   ↓
   ProfessionalRepository.searchProfessionals()
   - Executes JPQL query
   - Returns List<Professional>
   ↓

5. Back to Service Layer
   ↓
   ProfessionalService.searchProfessionals()
   - Maps entities to DTOs using ProfessionalMapper
   - Returns List<ProfessionalDTO>
   ↓

6. Controller Response
   ↓
   ResponseEntity<List<ProfessionalDTO>>
   ↓

7. HTTP Response
   ↓
   200 OK with JSON array
```

## Component Responsibilities

### Controller Layer
**Responsibility**: Handle HTTP requests/responses
- Validate request parameters
- Call appropriate service methods
- Return proper HTTP status codes
- Handle API routing

### Service Layer
**Responsibility**: Business logic and orchestration

#### ProfessionalService
- CRUD operations
- Orchestrate between repositories and mappers
- Transaction management
- Business rule validation

#### ProfessionalSearchService
- Search logic implementation
- Filter matching
- Search strategy selection
- Result filtering

### Mapper Layer
**Responsibility**: Object transformation

#### ProfessionalMapper
- Entity ↔ DTO conversion
- Nested object mapping
- Collection transformations
- Skill/Service/SocialLink management

### Repository Layer
**Responsibility**: Data access
- Database queries
- JPQL/SQL operations
- Entity persistence
- Custom query methods

### DTO Layer
**Responsibility**: Data transfer

#### ProfessionalDTO
- API contract definition
- Data serialization
- Validation rules

#### ProfessionalSearchCriteria
- Search parameter encapsulation
- Type-safe search criteria
- Helper methods for logic

### Entity Layer
**Responsibility**: Domain model
- Database mapping
- Relationship definitions
- Constraints and validations

## Dependency Injection Flow

```
Spring Container
    │
    ├─ Creates ProfessionalRepository (JPA Proxy)
    │
    ├─ Creates SkillRepository (JPA Proxy)
    │
    ├─ Creates ProfessionalMapper
    │   └─ Injects SkillRepository
    │
    ├─ Creates ProfessionalSearchService
    │   └─ Injects ProfessionalRepository
    │
    ├─ Creates ProfessionalService
    │   ├─ Injects ProfessionalRepository
    │   ├─ Injects ProfessionalMapper
    │   └─ Injects ProfessionalSearchService
    │
    └─ Creates ProfessionalController
        ├─ Injects ProfessionalService
        └─ Injects ContactService
```

## Key Design Patterns Used

1. **Repository Pattern**
   - `ProfessionalRepository`, `SkillRepository`
   - Abstracts data access

2. **Builder Pattern**
   - `ProfessionalSearchCriteria.builder()`
   - Flexible object creation

3. **Mapper Pattern**
   - `ProfessionalMapper`
   - Separates entity and DTO concerns

4. **Strategy Pattern**
   - Different search strategies (skills/category/general)
   - Implemented in `ProfessionalSearchService`

5. **Dependency Injection**
   - Constructor injection throughout
   - Loose coupling

6. **Factory Method**
   - `ResourceNotFoundException.professionalNotFound()`
   - Consistent exception creation

## Transaction Boundaries

```
Controller (No Transaction)
    ↓
Service (@Transactional)  ← Transaction starts here
    ↓
Repository (Inherits Transaction)
    ↓
Database Operations
    ↓
Service (@Transactional)  ← Transaction commits/rollbacks here
    ↓
Controller (No Transaction)
```

**Transactional Methods:**
- `createProfessional()`
- `updateProfessional()`

**Read-Only Methods:**
- `getAllProfessionals()`
- `getProfessionalById()`
- `searchProfessionals()`
- `getDistinctCities()`

## Error Handling Flow

```
Exception Thrown
    ↓
Service Layer
    ├─ ResourceNotFoundException
    │   └─ Professional not found
    │
    ├─ ValidationException
    │   └─ Invalid input data
    │
    └─ DataIntegrityViolationException
        └─ Database constraint violation
    ↓
Exception Handler (Future: @ControllerAdvice)
    ↓
HTTP Response
    ├─ 404 Not Found
    ├─ 400 Bad Request
    └─ 500 Internal Server Error
```

## Testing Strategy

### Unit Tests
```
ProfessionalMapperTest
    - Test entity → DTO conversion
    - Test DTO → entity conversion
    - Test updates

ProfessionalSearchServiceTest
    - Test search strategies
    - Test filter matching
    - Mock repository

ProfessionalServiceTest
    - Test CRUD operations
    - Mock mapper and search service
```

### Integration Tests
```
ProfessionalControllerIntegrationTest
    - Test complete API flow
    - Real database (H2/TestContainers)
    - Verify HTTP responses
```

This architecture provides a clean, maintainable, and testable foundation for the ProConnect backend application.
