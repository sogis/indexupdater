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

Der Indexupdater wird als Docker-Image bereitgestellt: https://hub.docker.com/r/sogis/indexupdater

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

Der Indexupdater wird ausschliesslich über HTTP-GET-Aufrufe genutzt. Es stehen die Pfade /update und /status zur Verfügung. /update für die Beauftragung eines neuen Aktualisierungsjobs, /status für die Abfrage des Status eines jobs, respektive für Abfrage des Zustands des Indexupdaters als ganzes.


### Pendent
Fertigstellung Benutzerdoku / Entwicklungsdokumentation


