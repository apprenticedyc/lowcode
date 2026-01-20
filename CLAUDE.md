# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an AI-powered low-code platform backend that generates web applications from natural language descriptions. Built with Spring Boot 3.5.8 and Java 21, it uses LangChain4j to integrate with various AI models (OpenAI, Claude, DeepSeek, etc.) for code generation.

## Common Development Commands

### Build & Run
```bash
# Build the project
mvn clean install

# Skip tests during build
mvn clean install -DskipTests

# Run the application
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Testing
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AiCodeGeneratorServiceTest

# Run with coverage
mvn test jacoco:report
```

### Database Operations
```bash
# Initialize database (run before first start)
mysql -u root -p -e "CREATE DATABASE \`ai-lowcode\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p ai-lowcode < sql/create_tale.sql
```

## Architecture Overview

### Core Components

1. **AI Generation Pipeline**
   - `AiCodeGeneratorFacade`: Main entry point for code generation
   - `AiCodeGeneratorServiceFactory`: Creates services based on generation type
   - Three generation modes: HTML, Multi-file, Vue projects

2. **Code Processing Chain**
   - **Parsers**: `CodeParser` implementations for different formats
     - `HtmlCodeParser`: Handles single-page HTML applications
     - `MultiFileCodeParser`: Processes multi-file projects
   - **Savers**: `CodeFileSaver` implementations using strategy pattern
     - `HtmlCodeFileSaver`: Saves HTML output
     - `MultiFileCodeFileSaver`: Saves structured projects

3. **Monitoring System**
   - `AiModelMetricsCollector`: Records AI model usage metrics
   - `AiModelMonitorListener`: Listens to AI model calls for tracking
   - Prometheus integration with custom metrics

4. **Security & Performance**
   - JWT authentication with role-based access (user/admin)
   - Redis-backed rate limiting using Redisson
   - Spring Session with Redis for distributed sessions
   - Caffeine for local caching

### Key Design Patterns

- **Facade Pattern**: `AiCodeGeneratorFacade` provides unified API
- **Factory Pattern**: Service creation based on `CodeGenTypeEnum`
- **Strategy Pattern**: Different parsers/savers for each generation type
- **Template Method**: `CodeFileSaverTemplate` defines save workflow

### Database Schema

Three main entities:
- `user`: User management with roles
- `app`: Application metadata and deployment keys
- `chat_history`: Conversation history for AI memory

## Development Guidelines

### Adding New Generation Types

1. Add enum in `CodeGenTypeEnum`
2. Create parser implementation in `core/parser/`
3. Create saver implementation in `core/saver/`
4. Update `AiCodeGeneratorService` for new type
5. Register in factory class

### Configuration

- Main config: `application.yaml`
- Local overrides: `application-local.yaml`
- AI model config in `langchain4j.open-ai.chat-model`
- Code output paths configurable under `app.code`

### Monitoring & Observability

- Metrics exposed at `/api/actuator/prometheus`
- Grafana dashboard in `docs/ai_model_grafana_config.json`
- Custom metrics for AI requests, token usage, response times

### Security Notes

- Authentication via JWT tokens
- Rate limiting with `@RateLimit` annotation
- All user inputs validated through guardrails
- Session timeout: 30 days (configurable)

## Important File Locations

- **AI Prompts**: `src/main/resources/prompt/`
- **Code Templates**: `core/builder/VueProjectBuilder.java`
- **Database Scripts**: `sql/create_tale.sql`
- **Nginx Config**: `src/main/resources/nginx.conf` (for deployment)
- **Monitoring Config**: `docs/` contains Prometheus and Grafana configs

## Deployment

Applications are deployed to `tmp/code_deploy/{deployKey}/` and served via Nginx. Each app gets a unique 6-character deploy key for access.

## Testing Strategy

- Integration tests use `@SpringBootTest`
- AI generation tests require valid AI model configuration
- Test database should be separate from development