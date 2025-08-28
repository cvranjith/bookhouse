# Library Management

## Problem Statement

You are asked to develop a Command Line Interface (CLI) to simulate the interaction of users with a small library system. The system allows users to borrow and return books, view their borrowed books, and manage their account.

The submission must include an executable `start.sh` file located at the root. When executed, this file should start the application and provide a CLI as demonstrated below. The `start.sh` is executed in an environment fully configured for Java (with the `java` executable in the `PATH` and `JAVA_HOME` environment variable pointing to the `JDK`), Node.js (with the `npm` and `node` executables in the `PATH`), and Dart (with the Dart SDK available and its executables in the `PATH`). All SDKs are set to their latest versions. The environment also has internet access, allowing package management tools to download additional dependencies as needed (e.g., `./gradlew build`, `./mvnw package`, `npm install`, `dart pub get`).

Each time `start.sh` is executed, it must create a new environment without reusing any data from previous runs. For instance, if `start.sh` is executed, and new users are created via the CLI, then the process is stopped and `start.sh` is executed again, the application should start fresh with no users registered.

## Commands

* `login [name]` - Logs in as this user and creates the user if they do not already exist
* `add [book_name]` - Adds a new book to the library (can only be done by the user `admin`)
* `list` - Displays the list of books in the library
* `borrow [book_name]` - Borrows a book for the logged-in user. The book is marked as borrowed and will not appear in the library's available list
* `return [book_name]` - Returns a borrowed book to the library. The book becomes available again
* `waitlist [book_name]` - Adds current user to the wait list of the book which is currently borrowed
* `status` - Displays a list of books currently borrowed by the logged-in user and his wait lists (if any)
* `logout` - Logs out of the current user

## Example Session

Your console output should contain at least the following output depending on the scenario and commands. But feel free 
to add extra output as you see fit.

```bash
$ login admin
Hello, admin!
You have access to library management.

$ list
No books are registered

$ add "The Great Gatsby"
Book "The Great Gatsby" has been added to the library.

$ add "1984"
Book "1984" has been added to the library.

$ logout
Goodbye, admin!

$ login Alice
Hello, Alice!
You don't have any books borrowed yet.

$ list
1. The Great Gatsby (available)
2. 1984 (available)

$ borrow "1984"
You borrowed "1984".

$ list
1. The Great Gatsby (available)
2. 1984 (borrowed)

$ status
Your borrowed books:
1. 1984

$ logout
Goodbye, Alice!

$ login Bob
Hello, Bob!
You don't have any books borrowed yet.

$ borrow "1985"
Sorry, "1985" is not registered.

$ borrow "1984"
Sorry, "1984" is currently not available.

$ waitlist "1985"
Sorry, "1985" is not registered.

$ waitlist "1984"
You are added to the wait list of "1984", your position is 1.

$ borrow "The Great Gatsby"
You borrowed "The Great Gatsby".

$ status
Your borrowed books:
1. The Great Gatsby
Your wait lists:
1. 1984 - position 1

$ logout
Goodbye, Bob!

$ login Carol
Hello, Carol!
You don't have any books borrowed yet.

$ waitlist "1984"
You are added to the wait list of "1984", your position is 2.
$ status
You don't have any books borrowed yet.
Your wait lists:
1. 1984 - position 2

$ logout
Goodbye, Carol!

$ login Alice
Hello, Alice!
Your borrowed books:
1. 1984

$ return "1984"
You returned "1984".

$ list
1. The Great Gatsby (borrowed)
2. 1984 (borrowed)

$ logout
Goodbye, Alice!

$ login Bob
You got the book "1984" from your waitlist
Your borrowed books:
1. The Great Gatsby
2. 1984

logout
Goodbye, Bob!

$ login Carol
Hello, Carol!
You don't have any books borrowed yet.
Your wait lists:
1. 1984 - position 1

$ list
1. The Great Gatsby (borrowed)
2. 1984 (borrowed)

$ return "The Great Gatsby"
Sorry, you didn't borrow "The Great Gatsby"
```
