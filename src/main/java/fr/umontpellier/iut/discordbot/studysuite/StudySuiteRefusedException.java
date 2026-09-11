package fr.umontpellier.iut.discordbot.studysuite;

import org.jetbrains.annotations.Nullable;

/** StudySuite a refusé la demande (compte en attente, groupe interdit, devoir introuvable…). */
public class StudySuiteRefusedException extends StudySuiteException {
    private final int status;
    @Nullable
    private final String code;

    public StudySuiteRefusedException(int status, @Nullable String code, @Nullable String message) {
        super(message == null ? "StudySuite refused the request (" + status + ")" : message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    @Nullable
    public String getCode() {
        return code;
    }
}
