# SmartSpend

SmartSpend is a comprehensive finance management application designed to help users track their income, expenses, savings, and goals. This README outlines the setup process, features of the app, and instructions for running automated tests using GitHub Actions.

## Table of Contents

- [Project Overview](#project-overview)
- [Features](#features)
- [Screenshots](#screenshots)
- [Installation](#installation)
- [Usage](#usage)
- [Documentation](#documentation)
- [Release Notes](#release-notes)
- [Useful Links](#useful-links)
- [License](#license)

## Project Overview

The SmartSpend API provides users with a platform to manage and control their finances in one place. Key features include:

- Viewing and managing expenses and income.
- Setting and tracking savings and purchasing goals.
- Notifications and reminders for financial activities.
- Customizable themes and category-specific color coding.
- Multi-language support (English and Afrikaans).

## Features

1. **Income and Expense Management**: Allows users to view, add, and categorize their financial transactions.
2. **Savings Goals**: Users can create, track, and achieve their savings goals.
3. **Notification System**: Automated notifications to keep users informed about their progress.
4. **Dark Mode**: A sleek dark theme with green accents designed to enhance the user experience.
5. **Category Color Coding**: Users can assign colors to different categories for better tracking and visualization.
6. **Biometric Login**: Secure fingerprint login for quick access.
7. **Clickable Goals with Detailed View**: Users can now click on goals to view more details.
8. **Confetti Celebration**: Fun confetti animation appears when a goal is 100% completed.
9. **Offline Sync with RoomDB**: Goals and Detailed View pages now use RoomDB for offline sync.
10. **Profile Stats**: Profile page now shows stats such as total goals completed.
11. **Category Editing Page**: New page to edit categories and improve customization.
12. **Multi-language Support**: Settings now support multiple languages (English and Afrikaans).

## Screenshots

Below are some screenshots of key parts of the SmartSpend app:

1. **Homepage**: Displays an overview of the user’s financial activities, including their expenses and income.
   
   ![Homepage Screenshot](Screenshots/homepage.png)

2. **Detailed View**: Shows detailed information about a specific category, including all associated expenses and income.
   
   ![Detailed View Screenshot](Screenshots/detailedview.png)

3. **Goals Page**: Allows users to create, track, and manage their savings goals.
   
   ![Goals Page Screenshot](Screenshots/goals.png)

4. **APK Signature Verification**: Demonstrates that our APK has been signed with a keystore file.
   
   ![APK Signature Screenshot](Screenshots/apk_signature.png)

## Installation

To set up the project locally, follow these steps:

1. Clone the repository:

    ```bash
    git clone https://github.com/MarcoMeyer1/SmartSpend
    ```

2. Navigate into the project directory:

    ```bash
    cd smartspend
    ```

3. Install dependencies:

    ```bash
    ./gradlew build
    ```

## Usage

1. Open the app and log in using your credentials.
2. Add income and expense transactions in their respective sections.
3. Set savings goals and track your progress in the **Goals** section.
4. Manage and view detailed reports of your finances in the **Detailed View** section.

## Documentation

### Purpose of the App
SmartSpend is a way for you to manage and control your finances all in one application. It is a great way to see how you are spending your money and see where it is all going. This gives all the users a way to control and track all their finances, from seeing your expenses and income history to setting saving and purchasing goals. SmartSpend is a brilliant and efficient way to track all of these things while having an easy time navigating the application.

### Design Considerations
The SmartSpend team has considered that a dark theme would suit this application, the light black and green tones compliments the application well. We have also added a color feature for our categories so the user can easily track what they are spending their money on for each category. It gives the user a better experience seeing the color they have designated for that category instead of searching for it if each category was the same.

### Utilisation of GitHub and GitHub Actions
GitHub Actions are used to automatically trigger unit tests upon each commit, ensuring code quality is maintained throughout the development process. The use of `dorny/test-reporter@v1` neatly formats the test results, providing clear feedback on test success or failure directly within the GitHub interface.

## Release Notes

### Version 2.0
- **Biometric Login**: Added fingerprint login for enhanced security and faster access.
- **Clickable Goals**: Goals are now clickable, leading to a detailed view for easier goal management.
- **Confetti Celebration**: Added a confetti animation when a goal is fully achieved.
- **Offline Sync with RoomDB**: Implemented RoomDB for offline sync on the Goals and Detailed View pages.
- **Multi-language Support**: Settings now include multi-language options (English and Afrikaans).
- **Profile Stats**: Profile page revamped to show statistics such as total goals completed.
- **Category Editing**: Added a new page to edit categories for more flexibility.
- **APK Signature**: Our APK is now signed, with proof included in the Screenshots folder.

## Useful Links
- **Our Group Members**: ST10038846, ST10157649, ST10050798
- **SmartSpend App Repo**: [SmartSpend Repository](https://github.com/MarcoMeyer1/SmartSpend)
- **SmartSpend API Repo**: [SmartSpend API Repository](https://github.com/MarcoMeyer1/SmartSpendAPI)
- **Youtube Presentation**: [SmartSpend Presentation](https://www.youtube.com/watch?v=RSxYxgywO40)

## License

This project is licensed under the MIT License - see the LICENSE file for details.
