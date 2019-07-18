# INDEXUPDATER

Http service coordinating entity based index reloading for solr. For an entity, it first deletes all documents of this entity, and then reloads them from source using the solr DataImportHandler (DIH).

Indexupdater has two main parts:
* "JobInput": Receives http job requests and puts them in a queue.
* "Backgroundworker": Takes the oldest job from the queue and performs the index update for the entity

Further documentation in german...

## Zusammenfassung

Uebersetzung der obenstehenden einleitenden Sätze:
Der indexupdater ist ein http-Service, welcher das neu Laden aller Dokumente einer "entity" koordiniert. Zuerst werden alle Dokumente einer entity gelöscht. Anschliessend werden die Dokumente mittels DataImportHandler (DIH) neu geladen. 

Der Indexupdater besteht aus den folgenden beiden Hauptteilen:
* "JobInput": Empfängt die http Job requests und legt die neuen jobs in eine Queue.
* "Backgroundworker": Nimmt den jeweils ältesten Job aus der Queue und führt die Indexaktualisierung aus.


