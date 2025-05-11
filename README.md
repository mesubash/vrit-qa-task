# VRIT QA Task

This repository contains a Selenium-based automation testing project for verifying the account setup process on a web application. The project is built using Java, Selenium WebDriver, and JUnit 5, and it includes utilities for handling temporary email creation and OTP verification.

## Features

- Automated testing of the account setup process.
- Temporary email generation using Mailinator.
- OTP retrieval and verification.
- Form filling for personal details, agency details, experience, and business registration.
- File upload handling for business documents.
- Comprehensive logging for debugging and test result analysis.

## Prerequisites

Before running the project, ensure you have the following installed:

- Java Development Kit (JDK) 17 or higher.
- Maven (for dependency management and build).
- Chrome browser (latest version).
- ChromeDriver (managed automatically by WebDriverManager).

## Project Structure

- **`src/test/java/com/vrit/qa/AccountSetupTest.java`**: Main test class for automating the account setup process.
- **`src/test/resources/test-docs/`**: Directory containing test documents for file uploads.
- **`pom.xml`**: Maven configuration file for managing dependencies and build plugins.
- **`target/`**: Directory containing compiled classes, test reports, and other build artifacts.

## Dependencies

The project uses the following dependencies:

- **Selenium WebDriver**: For browser automation.
- **WebDriverManager**: For managing browser drivers.
- **JUnit 5**: For writing and running tests.
- **Apache Commons Lang**: For generating random data.
- **JSON**: For handling temporary email creation.
- **SLF4J**: For logging.

Refer to the `pom.xml` file for the complete list of dependencies and their versions.

## Setup Instructions

1. Clone the repository:

   ```bash
   git clone git@github.com:mesubash/vrit-qa-task.git
   cd vrit-qa-task
   ```

2. Build the project using Maven:

   ```bash
   mvn clean install
   ```

3. Run the tests:

   ```bash
   mvn test
   ```

## Test Execution

The main test class is `AccountSetupTest`. It automates the following steps:

1. Navigates to the registration page.
2. Fills in personal details.
3. Retrieves and verifies OTP using Mailinator.
4. Completes agency details, experience, and business registration forms.
5. Submits the forms and verifies successful navigation.

Test results are generated in the `target/surefire-reports` directory.

## Logs and Reports

- **Test Reports**: Located in `target/surefire-reports/`.
- **Execution Logs**: Printed to the console during test execution.

## Troubleshooting

- Ensure the Chrome browser version matches the ChromeDriver version managed by WebDriverManager.
- If tests fail due to timeouts, increase the wait durations in the `WebDriverWait` configuration.
- Check the `target/surefire-reports` directory for detailed logs and error messages.

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Commit your changes and push them to your fork.
4. Submit a pull request with a detailed description of your changes.

## Contact

For any questions or issues, please contact [subash](mailto:subashdhamee@gmail.com).
