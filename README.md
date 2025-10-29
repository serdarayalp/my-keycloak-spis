# My Keycloak SPIs

## **Authentication Flow (Browser Flow)**
Ein Main Flow (also der gesamte Authentication Flow) gilt nur dann als erfolgreich,
wenn alle relevanten Executions und Subflows erfolgreich abgeschlossen sind —
aber was "relevant" ist, hängt davon ab, ob sie REQUIRED, ALTERNATIVE oder OPTIONAL markiert sind.

Ein Flow ist nur dann erfolgreich, wenn alle seine Bestandteile (Executions oder Subflows)
gemäß ihren Requirement-Typen — korrekt abgeschlossen sind.

Keycloak bewertet die Ergebnisse eines Flows in folgender Reihenfolge:

```
boolean requiredFailed = false;
boolean alternativeSucceeded = false;

for (Execution exec : flow.getExecutions()) {
    switch (exec.requirement) {
        case REQUIRED:
            if (!exec.success) {
                requiredFailed = true;
            }
            break;
        case ALTERNATIVE:
            if (exec.success) {
                alternativeSucceeded = true;
            }
            break;
        case OPTIONAL:
            // wird ignoriert, wenn nicht aktiv
            break;
    }
}

// Am Ende:
if (requiredFailed) failFlow();
else if (hasAlternative && !alternativeSucceeded) failFlow();
else successFlow();
```

Keycloak ignoriert nicht einfach einen Required-Schritt, nur weil etwas anderes erfolgreich war.

**REQUIRED** = Der Benutzer muss diese Execution bestehen, sonst schlägt der ganze Flow fehl.
**ALTERNATIVE** = Wenn eine Alternative erfolgreich ist, werden die restlichen übersprungen.
**OPTIONAL** = Wird ignoriert, wenn sie nicht relevant oder konfiguriert ist.
**DISABLED** = Wird komplett übersprungen.

Ein Flow ist nur erfolgreich, wenn

* alle REQUIRED erfolgreich sind,
* und mindestens eine ALTERNATIVE erfolgreich ist (falls vorhanden).

Keycloak behandelt den Flow als eine Liste von Executions und prüft jede in der angegebenen Reihenfolge.
Es beendet den Flow nicht automatisch nach dem ersten Erfolg, weil innerhalb desselben Flows theoretisch noch weitere Required-Schritte kommen könnten.

Erst wenn alle Executions "evaluated" sind (also geprüft, ob sie erfolgreich/irrelevant sind),
entscheidet Keycloak am Ende des Durchlaufs, ob der Flow als Ganzes "success" oder "fail" ist.

## **Häufige Stolperfallen**
- "Alternative" heißt nicht gleichzeitig. Du musst selbst UI-Links oder Buttons anbieten, damit der User auswählen kann.
  (z. B. per Freemarker-Template <a href="${url.loginAction}?execution=<id>">)
- "Required" vs. "Alternative" mischen ist gefährlich. Wenn eine Execution im selben Flow "Required" ist, wird sie immer verlangt, auch wenn eine Alternative erfolgreich war.
- Wenn du Reihenfolge änderst, bestimmt das, welche Methode zuerst angezeigt wird.
