# ProConnect Backend - Enhanced Search API Documentation

## Overview
The search API now supports comprehensive, elastic-like search functionality across multiple fields including professional profiles, skills, categories, services, and location data.

## Endpoint
`GET /api/professionals`

## Search Capabilities

### 1. **Elastic Search (Query Parameter: `q`)**
Searches across multiple fields simultaneously:
- Professional's first name, last name, headline, and bio
- Skill names and categories
- Service titles and descriptions

**Example:**
```bash
# Find anyone related to "plumbing"
GET /api/professionals?q=plumbing

# Find anyone related to "photography"
GET /api/professionals?q=photography

# Find by service keyword
GET /api/professionals?q=wedding
```

### 2. **Skill-Based Search**
Filter professionals by specific skill names.

**Parameters:**
- `skills` (array): List of exact skill names

**Example:**
```bash
# Find professionals with specific skills
GET /api/professionals?skills=Residential%20Plumbing&skills=Drain%20Cleaning
```

### 3. **Category-Based Search**
Filter professionals by skill categories.

**Parameters:**
- `categories` (array): List of skill categories

**Example:**
```bash
# Find all photographers
GET /api/professionals?categories=Photography

# Find all electricians and plumbers
GET /api/professionals?categories=Electrical&categories=Plumbing
```

### 4. **Location-Based Search**
Filter by geographical location at multiple levels.

**Parameters:**
- `city` (string): Filter by city name
- `state` (string): Filter by state/province
- `country` (string): Filter by country
- `remote` (boolean): Filter by remote work availability

**Examples:**
```bash
# Find professionals in New York
GET /api/professionals?city=New%20York

# Find professionals in California
GET /api/professionals?state=CA

# Find professionals in USA
GET /api/professionals?country=USA

# Find remote workers only
GET /api/professionals?remote=true
```

### 5. **Availability Filter**
Filter by professional availability status.

**Parameters:**
- `available` (boolean): Filter by availability

**Example:**
```bash
# Find only available professionals
GET /api/professionals?available=true
```

### 6. **Combined Searches**
All search parameters can be combined for precise filtering.

**Examples:**
```bash
# Photography services in USA
GET /api/professionals?q=photography&country=USA

# Available plumbers in New York
GET /api/professionals?q=plumbing&city=New%20York&available=true

# Remote electrical engineers
GET /api/professionals?q=electrical&remote=true

# Engineers in Karnataka state
GET /api/professionals?q=engineer&state=Karnataka
```

## Query Parameters Reference

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `q` | string | Elastic search across all text fields | `q=plumbing` |
| `city` | string | Filter by city | `city=New York` |
| `state` | string | Filter by state/province | `state=Karnataka` |
| `country` | string | Filter by country | `country=USA` |
| `remote` | boolean | Filter by remote work | `remote=true` |
| `available` | boolean | Filter by availability | `available=true` |
| `skills` | string[] | Filter by exact skill names | `skills=Residential Plumbing` |
| `categories` | string[] | Filter by skill categories | `categories=Photography` |

## Response Format

```json
[
  {
    "id": 1,
    "firstName": "John",
    "lastName": "Smith",
    "displayName": "John Smith",
    "headline": "Expert Plumber",
    "bio": "Professional plumbing services...",
    "avatarUrl": null,
    "coverImageUrl": null,
    "location": {
      "city": "New York",
      "state": "NY",
      "country": "USA",
      "remote": false
    },
    "isVerified": false,
    "isAvailable": true,
    "rating": null,
    "reviewCount": 0,
    "hourlyRateMin": null,
    "hourlyRateMax": null,
    "currency": "USD",
    "skills": [
      {
        "id": 29,
        "name": "Residential Plumbing",
        "category": "Plumbing"
      }
    ],
    "services": [
      {
        "id": 1,
        "title": "Drain Cleaning Service",
        "description": "Professional drain cleaning...",
        "priceMin": 100,
        "priceMax": 200,
        "currency": "USD",
        "priceUnit": "per job",
        "duration": "1-2 hours"
      }
    ],
    "socialLinks": []
  }
]
```

## Search Algorithm

### Priority Order:
1. **Exact skill matches** (if `skills` parameter provided)
2. **Category matches** (if `categories` parameter provided)
3. **Elastic search** (if `q` parameter provided):
   - Searches in: firstName, lastName, headline, bio
   - Searches in: skill names and categories
   - Searches in: service titles and descriptions
4. **Location filters** applied after initial search
5. **Availability filters** applied last

### Case Insensitivity
All text searches are case-insensitive.

### Partial Matching
The `q` parameter uses LIKE matching, so partial words match:
- `q=photo` matches "Photography", "Photographer", "Photo Studio"
- `q=plumb` matches "Plumbing", "Plumber"

## Use Cases

### Find Local Service Providers
```bash
# Find plumbers in my city
GET /api/professionals?q=plumber&city=Chicago&available=true
```

### Find Remote Workers
```bash
# Find remote software engineers in India
GET /api/professionals?q=engineer&country=India&remote=true
```

### Find by Specific Service
```bash
# Find wedding photographers
GET /api/professionals?q=wedding&categories=Photography
```

### Browse by Category
```bash
# See all electrical professionals
GET /api/professionals?categories=Electrical
```

## Performance Notes

- Searches use database indexes on frequently queried fields (city, availability)
- DISTINCT queries prevent duplicate results when joining skills/services
- LEFT JOINs ensure professionals without skills/services are still searchable

## Status Codes

- `200 OK`: Successful search (returns array, may be empty)
- `500 Internal Server Error`: Server error (check logs)

## Examples with cURL

```bash
# Basic search
curl "http://localhost:8080/api/professionals?q=plumbing"

# Location search
curl "http://localhost:8080/api/professionals?city=New%20York&available=true"

# Skill search
curl "http://localhost:8080/api/professionals?skills=Wedding%20Photography"

# Combined search
curl "http://localhost:8080/api/professionals?q=photography&country=USA&remote=true"
```

## Migration from Old API

### Before:
```bash
GET /api/professionals?q=plumber&city=NYC&available=true&skills=Plumbing
```

### Now (Enhanced):
```bash
# More flexible - search works across more fields
GET /api/professionals?q=plumber&city=NYC&available=true

# Or use specific skill filtering
GET /api/professionals?skills=Residential%20Plumbing&city=NYC&available=true

# Or use category filtering
GET /api/professionals?categories=Plumbing&city=NYC&available=true
```

## Future Enhancements

Potential future additions:
- Geolocation-based search (distance from coordinates)
- Price range filtering
- Rating/review filtering
- Sorting options (by rating, price, distance)
- Full-text search with relevance scoring
- Fuzzy matching for typo tolerance
