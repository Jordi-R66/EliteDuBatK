package fr.umontpellier.iut.discordbot.studysuite;

/** Le membre n'a pas de compte StudySuite lié à son compte Discord : il doit se connecter une fois sur le site. */
public class StudySuiteNotLinkedException extends StudySuiteException {
    public StudySuiteNotLinkedException() {
        super("No StudySuite account is linked to this Discord user");
    }
}
