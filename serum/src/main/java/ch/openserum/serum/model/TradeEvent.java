package ch.openserum.serum.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.p2p.solanaj.core.PublicKey;

import java.util.Arrays;

/**
 * Represents a Trade Event that occurs inside of a Serum {@link EventQueue}
 */
@Getter
@Setter
@NoArgsConstructor
public class TradeEvent {

    private PublicKey openOrders;
    private long nativeQuantityPaid;
    private byte[] orderId;
    private EventQueueFlags eventQueueFlags;
    private byte openOrdersSlot;
    private byte feeTier;
    private long nativeQuantityReleased;
    private long nativeFeeOrRebate;
    private long clientOrderId;

    private float floatPrice;
    private float floatQuantity;

    public TradeEvent(PublicKey openOrders, long nativeQuantityPaid, byte[] orderId, EventQueueFlags eventQueueFlags) {
        this.openOrders = openOrders;
        this.nativeQuantityPaid = nativeQuantityPaid;
        this.orderId = orderId;
        this.eventQueueFlags = eventQueueFlags;
    }

    @Override
    public String toString() {
        return "TradeEvent{" +
                "openOrders=" + openOrders +
                ", nativeQuantityPaid=" + nativeQuantityPaid +
                ", orderId=" + Arrays.toString(orderId) +
                ", eventQueueFlags=" + eventQueueFlags +
                ", openOrdersSlot=" + openOrdersSlot +
                ", feeTier=" + feeTier +
                ", nativeQuantityReleased=" + nativeQuantityReleased +
                ", nativeFeeOrRebate=" + nativeFeeOrRebate +
                ", clientOrderId=" + clientOrderId +
                ", floatPrice=" + floatPrice +
                ", floatQuantity=" + floatQuantity +
                '}';
    }
}
