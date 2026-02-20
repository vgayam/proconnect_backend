# ProConnect Backend API

RESTful API for ProConnect - Professional Services Discovery Platform

Built with **Spring Boot 3.2**, **PostgreSQL 15**, and **Java 17**

## 🚀 Quick Start

### Prerequisites
- **Java 17** or higher
- **Maven 3.6+** (or use Docker)
- **PostgreSQL 15+** (or use Docker Compose)

### Option 1: Run with Maven (Local)

1. **Install PostgreSQL and create database:**
```bash
# Install PostgreSQL (macOS)
brew install postgresql@15
brew services start postgresql@15

# Create database
psql postgres
CREATE DATABASE proconnect;
CREATE USER proconnect WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE proconnect TO proconnect;
\q
```

2. **Run the application:**
```bash
# Install dependencies and run
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/proconnect-backend-1.0.0.jar
```

API will be available at: `http://localhost:8080`

### Option 2: Run with Docker Compose (Recommended)

```bash
# Start both database and backend
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## 📋 API Endpoints

### Professionals
- `GET /api/professionals` - List all professionals
- `GET /api/professionals?q=photographer&city=Austin&available=true` - Search
- `GET /api/professionals?skills=Plumbing,Electrical` - Filter by skills
- `GET /api/professionals/{id}` - Get professional details
- `POST /api/professionals` - Create new professional
- `PUT /api/professionals/{id}` - Update professional
- `DELETE /api/professionals/{id}` - Delete professional

### Skills
- `GET /api/skills` - List all skills
- `GET /api/skills/categories` - Get skill categories
- `GET /api/skills/category/{category}` - Skills by category
- `POST /api/skills` - Create new skill

### Contact
- `POST /api/professionals/{id}/contact` - Send contact message

### Example Request
```bash
# Search for available electricians
curl "http://localhost:8080/api/professionals?skills=Electrical&available=true"

# Get professional details
curl "http://localhost:8080/api/professionals/1"

# Contact a professional
curl -X POST http://localhost:8080/api/professionals/1/contact \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "subject": "Need electrical work",
    "message": "I need help with home wiring"
  }'
```

## 🗄️ Database Schema

The database schema is automatically created on startup. See `src/main/resources/schema.sql` for details.

**Tables:**
- `professionals` - Professional profiles
- `skills` - Available skills/services
- `professional_skills` - Many-to-many relationship
- `services` - Services offered by professionals
- `social_links` - Social media links
- `contact_messages` - Contact form submissions

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/proconnect` |
| `DB_USERNAME` | Database username | `proconnect` |
| `DB_PASSWORD` | Database password | `password` |
| `FRONTEND_URL` | Frontend URL for CORS | `http://localhost:3000` |

### Update CORS Settings

Edit `src/main/resources/application.properties`:
```properties
cors.allowed-origins=http://localhost:3000,https://yourfrontend.vercel.app
```

## 🚀 Deployment

### Deploy to Railway

1. Push code to GitHub
2. Create new project on [Railway](https://railway.app)
3. Add PostgreSQL database
4. Connect GitHub repository
5. Set environment variables:
   - `DATABASE_URL` (auto-filled by Railway)
   - `FRONTEND_URL` (your Vercel URL)
6. Deploy!

### Deploy to Render

1. Create new Web Service on [Render](https://render.com)
2. Connect GitHub repository
3. Create PostgreSQL database
4. Link database to service
5. Set environment variables
6. Deploy

### Deploy to AWS/Heroku

See deployment guides in the docs.

## 🛠️ Development

### Run Tests
```bash
mvn test
```

### Build for Production
```bash
mvn clean package -DskipTests
```

### Database Migrations

The app uses Spring Boot's automatic schema generation. For production, consider using Flyway or Liquibase.

## 📦 Tech Stack

- **Spring Boot 3.2** - Application framework
- **Spring Data JPA** - Database access
- **PostgreSQL 15** - Database
- **Lombok** - Reduce boilerplate code
- **Maven** - Build tool
- **Docker** - Containerization

## 🔗 Connect with Frontend

Update your Next.js frontend to use this API:

1. Set environment variable in Vercel:
```
NEXT_PUBLIC_API_URL=https://your-backend-url.com
```

2. Create API client in `src/lib/api.ts`:
```typescript
const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export async function getProfessionals(params: SearchParams) {
  const query = new URLSearchParams();
  if (params.q) query.set('q', params.q);
  if (params.skills?.length) query.set('skills', params.skills.join(','));
  if (params.available) query.set('available', 'true');
  
  const response = await fetch(`${API_URL}/api/professionals?${query}`);
  return response.json();
}
```

## 📄 License

MIT License

## 👨‍💻 Author

Built for ProConnect Platform
