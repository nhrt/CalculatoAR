# CalculatoAR
Developing a formula solving AR app


*************Git-Befehle********************************************

Zu Beginn einer neuen Featureentwicklung:

git fetch
git checkout <branchname>
git rebase origin/...
//gegebenenfalls Konflikte manuell beseitigen
//dann fortsetzen mit git rebase --continue

Zum Ende einer neuen Featureentwicklung:
git fetch
git checkout <branchname>
git rebase origin/....
//gegebenenfalls Konflikte manuell beseitigen
//dann fortsetzen mit git rebase --continue 
git push origin <branchname>
//Wichtig: Erst nach endgültigem Abschluss pushen, sonst gibt es Probleme mit git rebase
//Sonst kann dieses nicht verwendet werden.
Merge-Request erstellen
  
 Quelle: https://wiki.thm.de/SWT-P_SS_2016_Augmented_Reality(1)
 
 Commits zusammenfassen:
 
 Man kann mit einem interaktiven Rebase auch mehrere Commits zu einem einzelnen Commit zusammenfassen. Im Skript der Rebase-Nachricht steht eine Anleitung, wie Du dazu vorgehen musst.
 
Wenn Du statt „pick“ oder „edit“, den Befehl „squash“ angibst, führt Git beide Commits zu einem gemeinsamen Commit zusammen und bietet Dir die Möglichkeit, die Commit-Nachricht ebenso entsprechend zu verheiraten. Wenn Du also aus den drei Commits einen einzelnen Commit machen willst, muss Dein Skript folgendermaßen aufgebaut sein:

pick f7f3f6d changed my name a bit
squash 310154e updated README formatting and added blame
squash a5f4a0d added cat-file

Nach dem Speichern und Beenden des Editors, führt Git alle drei Änderungen zu einem einzelnen Commit zusammen und öffnet einen Texteditor, der alle drei Commit-Nachrichten enthält

Quelle: https://git-scm.com/book/de/v1/Git-Tools-%C3%84nderungshistorie-ver%C3%A4ndern
 
 Bei Merge-Conflict:
 
Git führt beim Merge Änderungen automatisch zusammen, sofern unterschiedliche Dateien oder unterschiedliche Stellen in der selben Datei betroffen sind. Wurden in beiden Branches Änderungen an denselben Code-Zeilen committet, führt dies beim Merge zum Konflikt.
 
git versieht dann die konfliktbehafteten Stellen in den Dateien mit Markern - oben steht der aktive Branch, unten der zu mergende Branch

Um den Merge abzuschließen, müssen die Konflikte behoben, die Marker entfernt, die Dateien dem Index hinzugefügt und die Änderungen committet werden.

Dabei ist zu beachten, dass Git währenddessen im Merge-Modus ist, zu erkennen am Kommandozeilen-Prompt. Dieser Modus wird erst wieder verlassen, wenn der Merge durchgeführt wurde; keinesfalls sollte man in diesem Zustand weiterarbeiten. Alternativ kann der Merge abgebrochen werden mit: $ git merge --abort

Praktischerweise ist wiederholtes Mergen bei Git kein Problem. Da die bereits erfolgten Merges in der Historie dokumentiert sind, wird git nur die noch fehlenden Commits übernehmen.

Quelle: https://www.ralfebert.de/git/mergekonflikte-beheben/
 
 ********************************************************************
 
 
 
