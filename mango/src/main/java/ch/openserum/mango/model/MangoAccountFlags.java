package ch.openserum.mango.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class MangoAccountFlags {
    boolean initialized;
    boolean mangoGroup;
    boolean marginAccount;
    boolean mangoSrmAccount;
}