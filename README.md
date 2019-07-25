# INDEXUPDATER

Http service coordinating entity based index reloading for solr. For an entity, it first deletes all documents of this entity, and then reloads them from source using the solr DataImportHandler (DIH).

Indexupdater has two main parts:
* "JobInput": Receives http job requests and puts them in a queue.
* "Backgroundworker": Takes the oldest job from the queue and performs the index update for the entity

Further documentation in german...

## Zusammenfassung

Uebersetzung der obenstehenden einleitenden englischen Kapitels:

Der Indexupdater ist ein http-Service, welcher das neu Laden aller Dokumente einer "entity" koordiniert. Zuerst werden alle Dokumente einer entity gelöscht. Anschliessend werden die Dokumente mittels DataImportHandler (DIH) neu geladen. 

Der Indexupdater besteht aus den folgenden beiden Hauptteilen:
* "JobInput": Empfängt die http Job requests und legt die neuen jobs in eine Queue.
* "Backgroundworker": Nimmt den jeweils ältesten Job aus der Queue und führt die Indexaktualisierung aus.

## Installation

Der Indexupdater wird als Docker-Image bereitgestellt: https://hub.docker.com/r/sogis/indexupdater. Der im Docker Image genutzte Webserver startet auf Port 8080. 

Die Konfiguration erfolgt über die Umgebungsvariable SPRING\_APPLICATION\_JSON. Parameter:
* solrProtocol: http oder https
* solrHost: Der Hostname des anzusprechenden Solr-Servers
* solrPort: Der Port des anzusprechenden Solr-Servers
* solrPathQuery: Der Pfad, auf welchem "selects" an den entsprechenden Index, resp. die Collection geschickt werden kann.
* solrPathUpdate: Der Pfad, auf welchem "updates" an den entsprechenden Index, resp. die Collection geschickt werden kann.
* logSilenceMaxDurationSeconds: Definiert, wie lange bei Inaktivität vom Backgroundworker kein Logeintrag geschrieben wird.
* dihPollIntervalSeconds: Bestimmt, in welchem Abstand bei einem laufenden DIH-Import eines Statusaktualisierung von Solr angefordert wird.
* dihImportMaxDurationSeconds: Timeout, nach welchem Solr angewiesen wird, einen laufenden Import abzubrechen.
* dihDefaultPath: Pfad zum Default-Importhandler. Parameter wird verwendet, sofern beim API-Aufruf der Importhandler nicht gesetzt ist.

Beispielkonfiguration:

```json
SPRING_APPLICATION_JSON='{"solrProtocol":"http","solrHost":"localhost","solrPort":8983,"solrPathQuery":"solr/gdi/select","solrPathUpdate":"solr/gdi/update","logSilenceMaxDurationSeconds":5,"dihPollIntervalSeconds":2,"dihImportMaxDurationSeconds":120,"dihDefaultPath":"solr/gdi/dih"}'
```

## Benutzung / API-Dokumentation

Der Indexupdater wird ausschliesslich über HTTP-GET-Aufrufe genutzt. Es stehen die Pfade /update und /status zur Verfügung. /update für die Beauftragung eines neuen Aktualisierungsjobs, /status für die Abfrage des Status eines jobs, respektive für die Abfrage des Zustands des Indexupdaters als ganzes.

### Pfad /queue

Ein GET auf den Pfad /queue mit korrekten URL-Parametern erstellt die Aktualisierungsjobs für die übergebenen entity's (Tabellen / Views). Pro übergebener entity wird ein Job in die Queue gelegt und anschliessend abgearbeitet.

Beispiel-Aufruf:

``` http
http://localhost:8080/queue?ds=ch.so.agi.fill_10k_60k,ch.so.agi.fill&timeout=3
```

#### Zwingende Parameter
* ds: Identifier des/der dataset(s), für welche der Index neu geladen werden soll. Bei der Uebergabe mehrerer Datasets werden diese mit Komma getrennt.


#### Optionale Parameter

Uebergebene optionale Parameter überschreiben jeweils den bei der Installation mittels Umgebungsvariable SPRING\_APPLICATION\_JSON gesetzten Wert.
* dih: Pfad des zu verwendenden Dataimporthandlers. Siehe auch Kapitel Installation, dihDefaultPath.
* poll: Siehe Kapitel Installation, dihPollIntervalSeconds
* timeout: Siehe Kapitel Installation, dihImportMaxDurationSeconds

#### Antwort

Json-Array der Identifier der erstellten Jobs.

### Pfad /status

Gibt eine menschenlesbare Antwort zum Status des ganzen Indexupdaters zurück.

### Pfad /status/{job identifier}

Gibt eine maschinen- und menschenlesbare Antwort zum Status des angefragten Jobs zurück.

Antwort-Codes in der Response (HTTP Status-Code 200):
* PENDING: Job ist beauftragt und wartet in der Queue auf die Bearbeitung. 
* WORKING: Job wird von Solr gegenwärtig ausgeführt.
* ENDED_OK: Job wurde erfolgreich abgeschlossen.
* ENDED_ABORTED: Job wurde wegen Timeout-Ueberschreitung abgebrochen. Index kann bezüglich des betroffenen Datasets inkonsistent sein.
* ENDED_EXCEPTION: Job wurde aufgrund des Auftretens eines Fehlers abgebrochen. Index kann bezüglich des betroffenen Datasets inkonsistent sein.

Bei der Uebergabe eines unbekannten Job-Identifiers wird HTTP Status-Code 404 "not found" zurückgegeben. Der Indexupdater merkt sich lediglich die letzten 20 ausgeführten Jobs. 

Beispiel-Aufruf:

```
http://localhost:8080/status/yP
```

#### Hinweis zum Job-Identifier

Zentral in der Benutzung des API's sind die sogenannten Job-Identifier. Diese werden automatisch erzeugt mit den Anforderungen "kurz, gut lesbar und eindeutig". Garantiert eindeutig sind sie jeweils in einer Periode von 7 Tagen. Ueber längere Perioden können Duplikate nicht ausgeschlossen werden.

In der Praxis ist diese Rahmenbedingung kaum relevant, da immer der jüngste auf den Identifier passende Job zurückgegeben wird. 

## Entwicklerdokumentation

Das Programm umfasst drei Hauptteile:
* Mittels **ch.so.agi.solr.indexupdater.Controller.addJobToQueue(...)** werden neue Jobs in die Queue gelegt.
* Im Package ch.so.agi.solr.indexupdater.model ist der State der hängigen, laufenden, und abgeschlossenen Jobs hinterlegt. Die **QueueOfJobs** ist die zentrale Collection, in welche addJobToQueue(...) neue Jobs reinlegt.
* Mittels **ch.so.agi.solr.indexupdater.QueuePoller.runOnePoll()** wird von einem von Spring verwalteten Background-Prozess periodisch die Queue auf neu zu verarbeitende Jobs abgefragt. Wenn welche vorhanden sind, wird der älteste wartende Job gezogen und bearbeitet.

### Job-Verarbeitung

Die Job-Verarbeitung umfasst die folgenden Schritte
* Alle Dokumente der entsprechenden entity aus dem Index löschen. Verwendet das Solr [index path]/update API.
* Die Dokumente der entity neu aus der Datenbank laden. Verwendet den DataImportHandler (DIH)
* Ob die Schritte korrekt abgelaufen sind wird mittels Zählen der Dokumente vor und nach jedem Schritt sichergestellt. Verwendet das Solr [index path]/select API

Die Verarbeitung wird von den Klassen des package **ch.so.agi.solr.indexupdater.jobexec** verrichtet. Hauptsächlich durch:
* **IndexSliceUpdater**: Umfasst den Code für das Update einer entity gemäss den obig beschriebenen Schritten. Siehe IndexSliceUpdater.execute()
* **DihPoller**: Fragt Solr periodisch nach dem Zustand des laufenden Import-Prozesses. Wird von IndexSliceUpdater aufgerufen.





