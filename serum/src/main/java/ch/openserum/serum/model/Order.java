package ch.openserum.serum.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.p2p.solanaj.core.PublicKey;

/**
 * Class that represents a Serum order.
 */
@Builder
@Getter
@Setter
@ToString
public class Order {

    private long price;
    private long quantity;
    private long clientOrderId;
    private float floatPrice;
    private float floatQuantity;
    private PublicKey owner;

    // used in newOrderv3. no constructor, only setters/getters
    private long maxQuoteQuantity;
    private long clientId;
    private OrderTypeLayout orderTypeLayout;
    private SelfTradeBehaviorLayout selfTradeBehaviorLayout;
    private boolean buy;

}
