
package org.glassfish.jersey.servlet.init.internal;

import org.glassfish.jersey.internal.l10n.Localizable;
import org.glassfish.jersey.internal.l10n.LocalizableMessageFactory;
import org.glassfish.jersey.internal.l10n.Localizer;


/**
 * Defines string formatting method for each constant in the resource file
 * 
 */
public final class LocalizationMessages {

    private final static LocalizableMessageFactory messageFactory = new LocalizableMessageFactory("org.glassfish.jersey.servlet.init.internal.localization");
    private final static Localizer localizer = new Localizer();

    public static Localizable localizableJERSEY_APP_REGISTERED_MAPPING(Object arg0, Object arg1) {
        return messageFactory.getMessage("jersey.app.registered.mapping", arg0, arg1);
    }

    /**
     * Registering the Jersey servlet application, named {0}, at the servlet mapping {1}, with the Application class of the same name.
     * 
     */
    public static String JERSEY_APP_REGISTERED_MAPPING(Object arg0, Object arg1) {
        return localizer.localize(localizableJERSEY_APP_REGISTERED_MAPPING(arg0, arg1));
    }

    public static Localizable localizableJERSEY_APP_REGISTERED_CLASSES(Object arg0, Object arg1) {
        return messageFactory.getMessage("jersey.app.registered.classes", arg0, arg1);
    }

    /**
     * Registering the Jersey servlet application, named {0}, with the following root resource and provider classes: {1}
     * 
     */
    public static String JERSEY_APP_REGISTERED_CLASSES(Object arg0, Object arg1) {
        return localizer.localize(localizableJERSEY_APP_REGISTERED_CLASSES(arg0, arg1));
    }

    public static Localizable localizableJERSEY_APP_NO_MAPPING_OR_ANNOTATION(Object arg0, Object arg1) {
        return messageFactory.getMessage("jersey.app.no.mapping.or.annotation", arg0, arg1);
    }

    /**
     * The Jersey servlet application, named {0}, is not annotated with {1} and has no servlet mapping.
     * 
     */
    public static String JERSEY_APP_NO_MAPPING_OR_ANNOTATION(Object arg0, Object arg1) {
        return localizer.localize(localizableJERSEY_APP_NO_MAPPING_OR_ANNOTATION(arg0, arg1));
    }

    public static Localizable localizableJERSEY_APP_NO_MAPPING(Object arg0) {
        return messageFactory.getMessage("jersey.app.no.mapping", arg0);
    }

    /**
     * The Jersey servlet application, named {0}, has no servlet mapping.
     * 
     */
    public static String JERSEY_APP_NO_MAPPING(Object arg0) {
        return localizer.localize(localizableJERSEY_APP_NO_MAPPING(arg0));
    }

    public static Localizable localizableJERSEY_APP_MAPPING_CONFLICT(Object arg0, Object arg1) {
        return messageFactory.getMessage("jersey.app.mapping.conflict", arg0, arg1);
    }

    /**
     * Mapping conflict. A Servlet registration exists with same mapping as the Jersey servlet application, named {0}, at the servlet mapping, {1}. The Jersey servlet is not deployed.
     * 
     */
    public static String JERSEY_APP_MAPPING_CONFLICT(Object arg0, Object arg1) {
        return localizer.localize(localizableJERSEY_APP_MAPPING_CONFLICT(arg0, arg1));
    }

    public static Localizable localizableJERSEY_APP_REGISTERED_APPLICATION(Object arg0) {
        return messageFactory.getMessage("jersey.app.registered.application", arg0);
    }

    /**
     * Registering the Jersey servlet application, named {0}, with the Application class of the same name.
     * 
     */
    public static String JERSEY_APP_REGISTERED_APPLICATION(Object arg0) {
        return localizer.localize(localizableJERSEY_APP_REGISTERED_APPLICATION(arg0));
    }

}
