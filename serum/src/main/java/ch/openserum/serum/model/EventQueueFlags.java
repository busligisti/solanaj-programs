package ch.openserum.serum.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Used to represent flags for Event Queue events
 */
@Getter
@ToString
@AllArgsConstructor
public class EventQueueFlags {

    private boolean fill;
    private boolean out;
    private boolean bid;
    private boolean maker;

}
