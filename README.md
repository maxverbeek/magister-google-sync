# Magister Google Calendar Sync 
Sync Magister rooster met Google Calendar

# Deze shit clonen
Je kan gewoon `git clone git@github.com:maxverbeek/magister-google-sync` doen, maar omdat er een submodule in zit is het aan te raden om `git clone --recursive git@github.com:maxverbeek/magister-google-sync` te doen. Je kan natuurlijk ook normaal clonen, en daarna `git submodule init` en `git submodule update` uit voeren.

# How to use
1. Maak een credential file in [de google console](https://console.developers.google.com/start/api?id=calendar) en download het bestand met json shit..
2. Zet dat bestand in `src/main/resources/client_secret.json`
3. Run `gradle -q run` in je terminal.

# Credentials
Je screts (`client_secret.json`) staat in gitignore. Als je `gradle -q run` voor de 1e keer uitvoert, worden je gegevens opgeslagen in `~/.credentials/calendar-java-quickstart/StoredCredential`. Je kunt dus gwn `git commit -am` doen, zonder je zorgen te maken dat heel github bij je calender kan.

