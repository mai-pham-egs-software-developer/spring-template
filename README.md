```text
spring-template/
└── backend/
	├── source/                         # Backend Java/Maven source code
	│   ├── pom.xml                     # Parent Maven project
	│   ├── application/                # Runnable application module
	│   │   ├── pom.xml
	│   │   └── src/
	│   │       ├── main/java/
	│   │       └── main/resources/
	│   └── modules/                    # Shared/reusable dependencies
	│       ├── security/
	│       │   ├── pom.xml
	│       │   └── src/
	│       ├── utility/
	│       │   ├── pom.xml
	│       │   └── src/
	│       └── audit/
	│           ├── pom.xml
	│           └── src/
	└── deployment/                     # Backend deployment configuration
		├── docker/
		├── kubernetes/
		└── README.md
```

## Maven module relationships

- `backend/source/pom.xml` is the parent and manages shared properties and dependencies.
- `backend/source/application` is the runnable application module.
- `backend/source/modules/*` contains reusable modules such as security, utility, and audit.
- `backend/deployment` contains deployment artifacts and environment configuration.
