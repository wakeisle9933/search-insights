# Search Insights Project 🔍📊

Hey there! Welcome to the Search Insights project! 🎉 This project analyzes Google Search Console and Google Analytics data to create reports! 😘

## Key Features ✨

1. Fetch Google Search Console data 🕵️‍♀️
2. Grab Google Analytics data 📈
3. Analyze and summarize data 🧮
4. Analyze the backlinks that came to your website 🥸
5. Generate Excel reports 📑
6. Send reports via email 📧

## Getting Started 🚀

### What You Need 📋

- Java 17 🍵
- Gradle 8.8 🐘
- Google Cloud project (Search Console API, Analytics API enabled) ☁️
- SMTP server setup (for sending emails) 📨

### Setup ⚙️

1. Copy `src/main/resources/application-example.properties` to `application.properties` and fill in the needed info! 💖

2. Create a service account key in your Google Cloud project and save it to `src/main/resources/credential/search-insights.json`! 🔑

3. Build the project with Gradle:
   ```
   ./gradlew build
   ```

4. Run it:
   ```
   java -jar build/libs/search-insights-0.0.1-SNAPSHOT.jar
   ```

## How to Use 🎮

### Manual Execution

When the program runs, it'll automatically fetch data, analyze it, create a report, and send it via email! 📧

You can get daily, weekly, and monthly reports:
- Daily: Data from 3 days ago
- Weekly (Wednesday): Last 7 days of data
- Monthly (3rd day): All data from last month

## Custom Reports 📊

Need data for a specific period? Use the `/email-search-insights-report` endpoint!

Example:
```
GET /email-search-insights-report?fromDate=2023-01-01&toDate=2023-01-31
```

## Run with Docker 🐳

You can build and run a Docker image:

```
docker build -t search-insights .
docker run --name search-insights search-insights
```

### Automate with Windows Task Scheduler 🕰️

Want to generate reports automatically every day? After creating the Docker image, use Windows Task Scheduler! 👀

1. Search for 'Task Scheduler' in the Start menu and run it. 🔍
2. Click 'Create Task' in the right panel. ➕
3. Give it a name (e.g., "Search Insights Daily Run") and set it to run daily in the Triggers tab. ⏰
4. In the Actions tab, click 'New' and enter this command:
   ```
   docker start search-insights
   ```
5. Click OK to save! ✅

This way, the container will start automatically every day, create and send the report, then stop itself! Saves resources too - win-win! 😉👍

## Contribute 💞

Wanna contribute to this project? You're always welcome! Send a PR and l'll review and merge it! 😘

---

Get awesome insights with this project! If you have any questions, just ask! 🎊🎉
