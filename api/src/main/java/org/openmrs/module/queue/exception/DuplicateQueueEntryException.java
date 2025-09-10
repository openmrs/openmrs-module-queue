package org.openmrs.module.queue.exception;

import org.openmrs.api.ValidationException;

import java.io.Serial;

/**
 * Exception thrown when a patient is already present in a queue
 * and another attempt is made to add them again.
 */
public class DuplicateQueueEntryException extends ValidationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Integer patientId;
    private final Integer queueId;

    /**
     * Creates a new DuplicateQueueEntryException with a plain message.
     *
     * @param message the error message
     */
    public DuplicateQueueEntryException(String message) {
        super(message);
        this.patientId = null;
        this.queueId = null;
    }

    /**
     * Creates a new DuplicateQueueEntryException with a message and a cause.
     *
     * @param message the error message
     * @param cause   the underlying cause of the exception
     */
    public DuplicateQueueEntryException(String message, Throwable cause) {
        super(message, cause);
        this.patientId = null;
        this.queueId = null;
    }

    /**
     * Creates a new DuplicateQueueEntryException with patient and queue IDs.
     *
     * @param patientId the ID of the patient already in the queue
     * @param queueId   the ID of the queue
     */
    public DuplicateQueueEntryException(Integer patientId, Integer queueId) {
        super("Patient with ID " + patientId + " is already in queue " + queueId);
        this.patientId = patientId;
        this.queueId = queueId;
    }

    /**
     * Creates a new DuplicateQueueEntryException with an i18n message code and arguments.
     *
     * @param messageCode the i18n message code
     * @param parameters  parameters for the message
     */
    public DuplicateQueueEntryException(String messageCode, Object... parameters) {
        super(messageCode, parameters);
        this.patientId = null;
        this.queueId = null;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public Integer getQueueId() {
        return queueId;
    }
}
