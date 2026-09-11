package fr.umontpellier.iut.discordbot.studysuite;

import fr.umontpellier.iut.discordbot.services.exceptions.ServiceException;

/** L'API StudySuite est injoignable ou a répondu autre chose que ce qui était attendu. */
public class StudySuiteException extends ServiceException {
    public StudySuiteException(String message) {
        super(message);
    }

    public StudySuiteException(String message, Throwable cause) {
        super(message, cause);
    }
}
