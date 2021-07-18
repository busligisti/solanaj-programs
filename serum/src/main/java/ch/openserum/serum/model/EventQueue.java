package ch.openserum.serum.model;

import lombok.Getter;
import lombok.Setter;
import org.bitcoinj.core.Utils;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.utils.ByteUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ch.openserum.serum.model.SerumUtils.U64_SIZE_BYTES;
import static ch.openserum.serum.model.SerumUtils.U128_SIZE_BYTES;

/*
const EVENT_QUEUE_HEADER = struct([
  blob(5), 0-5

  accountFlagsLayout('accountFlags'), 5-12
  u32('head'), 12-16
  zeros(4), 16-20
  u32('count'), 20-24
  zeros(4), 24-28
  u32('seqNum'), 28-32
  zeros(4), 32-36
]);
 */

/*
const EVENT_FLAGS = bits(u8(), false, 'eventFlags');
EVENT_FLAGS.addBoolean('fill');
EVENT_FLAGS.addBoolean('out');
EVENT_FLAGS.addBoolean('bid');
EVENT_FLAGS.addBoolean('maker');

const EVENT = struct([
  EVENT_FLAGS,
  u8('openOrdersSlot'),
  u8('feeTier'),
  blob(5),
  u64('nativeQuantityReleased'), // Amount the user received
  u64('nativeQuantityPaid'), // Amount the user paid
  u64('nativeFeeOrRebate'),
  u128('orderId'),
  publicKeyLayout('openOrders'),
  u64('clientOrderId'),
]);
 */

/**
 * Represents a Serum Event Queue
 */
@Getter
@Setter
public class EventQueue {

    // sizes
    private static final int HEADER_LAYOUT_SPAN = 37;
    private static final int NODE_LAYOUT_SPAN = 88;

    // offsets
    private static final int HEAD_OFFSET = 13;
    private static final int COUNT_OFFSET = 21;
    private static final int SEQ_NUM_OFFSET = 29;

    // event offsets
    private static final int NATIVE_QUANTITY_RELEASED_OFFSET = 8;
    private static final int NATIVE_QUANTITY_PAID_OFFSET = NATIVE_QUANTITY_RELEASED_OFFSET + U64_SIZE_BYTES;
    private static final int NATIVE_FEE_OR_REBATE_OFFSET = NATIVE_QUANTITY_PAID_OFFSET + U64_SIZE_BYTES;
    private static final int ORDER_ID_OFFSET = NATIVE_FEE_OR_REBATE_OFFSET + U64_SIZE_BYTES;
    private static final int OPEN_ORDERS_OFFSET = ORDER_ID_OFFSET + U128_SIZE_BYTES;
    private static final int CLIENT_ORDER_ID_OFFSET = OPEN_ORDERS_OFFSET + PublicKey.PUBLIC_KEY_LENGTH;

    private AccountFlags accountFlags;
    private int head;
    private int count;
    private int seqNum;
    private List<TradeEvent> events;

    private long baseLotSize;
    private long quoteLotSize;
    private byte baseDecimals;
    private byte quoteDecimals;

    /**
     * Returns an {@link EventQueue} object which is built from binary data.
     *
     * @param eventQueueData binary data
     * @return built {@link EventQueue} object
     */
    @SuppressWarnings("DuplicatedCode")
    public static EventQueue readEventQueue(byte[] eventQueueData, byte baseDecimals, byte quoteDecimals, long baseLotSize, long quoteLotSize) {
        EventQueue eventQueue = new EventQueue();
        List<TradeEvent> events = new ArrayList<>();
        eventQueue.setEvents(events);

        eventQueue.setBaseDecimals(baseDecimals);
        eventQueue.setQuoteDecimals(quoteDecimals);
        eventQueue.setBaseLotSize(baseLotSize);
        eventQueue.setQuoteLotSize(quoteLotSize);

        // Verify that the "serum" padding exists
        SerumUtils.validateSerumData(eventQueueData);

        // Read account flags
        AccountFlags accountFlags = AccountFlags.readAccountFlags(eventQueueData);
        eventQueue.setAccountFlags(accountFlags);

        // Read rest of EVENT_QUEUE_HEADER (head, count, seqNum ints)
        int head = (int) Utils.readUint32(eventQueueData, HEAD_OFFSET);
        int count = (int) Utils.readUint32(eventQueueData, COUNT_OFFSET);
        int seqNum = (int) Utils.readUint32(eventQueueData, SEQ_NUM_OFFSET);

        eventQueue.setHead(head);
        eventQueue.setCount(count);
        eventQueue.setSeqNum(seqNum);

        // allocLen = number of elements
        int allocLen = (eventQueueData.length - HEADER_LAYOUT_SPAN) / NODE_LAYOUT_SPAN;

        for (int i = 0; i < allocLen; ++i) {
            int nodeIndex = (head + count + allocLen - 1 - i) % allocLen;
            int eventOffset = HEADER_LAYOUT_SPAN + (nodeIndex * NODE_LAYOUT_SPAN);

            // read in 88 bytes of event queue data
            byte[] eventData = Arrays.copyOfRange(eventQueueData, eventOffset, eventOffset + NODE_LAYOUT_SPAN);
            byte eventFlags = eventData[0];
            boolean fill = (eventFlags & 1) == 1;
            boolean out = (eventFlags & 2) == 2;
            boolean bid = (eventFlags & 4) == 4;
            boolean maker = (eventFlags & 8) == 8;

            EventQueueFlags eventQueueFlags = new EventQueueFlags(fill, out, bid, maker);

            byte openOrdersSlot = eventData[1];
            byte feeTier = eventData[2];

            // blob = 3-7 - ignore
            // Amount the user received (quantity)
            long nativeQuantityReleased = ByteUtils.readUint64(eventData, NATIVE_QUANTITY_RELEASED_OFFSET).longValue();

            // Amount the user paid (price)
            long nativeQuantityPaid = ByteUtils.readUint64(eventData, NATIVE_QUANTITY_PAID_OFFSET).longValue();

            long nativeFeeOrRebate = ByteUtils.readUint64(eventData, NATIVE_FEE_OR_REBATE_OFFSET).longValue();
            byte[] orderId = Arrays.copyOfRange(eventData, ORDER_ID_OFFSET, OPEN_ORDERS_OFFSET);
            PublicKey openOrders = PublicKey.readPubkey(eventData, OPEN_ORDERS_OFFSET);
            long clientOrderId = ByteUtils.readUint64(eventData, CLIENT_ORDER_ID_OFFSET).longValue();

            if (fill && nativeQuantityPaid > 0) {
                TradeEvent tradeEvent = new TradeEvent();
                tradeEvent.setOpenOrders(openOrders);
                tradeEvent.setNativeQuantityPaid(nativeQuantityPaid);
                tradeEvent.setOrderId(orderId);
                tradeEvent.setEventQueueFlags(eventQueueFlags);
                tradeEvent.setOpenOrdersSlot(openOrdersSlot);
                tradeEvent.setFeeTier(feeTier);
                tradeEvent.setNativeQuantityReleased(nativeQuantityReleased);
                tradeEvent.setNativeFeeOrRebate(nativeFeeOrRebate);
                tradeEvent.setClientOrderId(clientOrderId);

                if (bid) {
                    double priceBeforeFees = maker ? nativeQuantityPaid + nativeFeeOrRebate : nativeQuantityPaid - nativeFeeOrRebate;
                    double top = priceBeforeFees * SerumUtils.getBaseSplTokenMultiplier(baseDecimals);
                    double bottom = SerumUtils.getQuoteSplTokenMultiplier(quoteDecimals) * nativeQuantityReleased;
                    float price = (float) (top / bottom);

                    tradeEvent.setFloatPrice(price);
                    tradeEvent.setFloatQuantity((float) (nativeQuantityReleased / SerumUtils.getBaseSplTokenMultiplier(baseDecimals)));

                } else {
                    double priceBeforeFees = maker ? nativeQuantityReleased - nativeFeeOrRebate : nativeQuantityReleased + nativeFeeOrRebate;
                    double top = priceBeforeFees * SerumUtils.getBaseSplTokenMultiplier(baseDecimals);
                    double bottom = SerumUtils.getQuoteSplTokenMultiplier(quoteDecimals) * nativeQuantityPaid;
                    float price = (float) (top / bottom);

                    tradeEvent.setFloatPrice(price);
                    tradeEvent.setFloatQuantity((float) (nativeQuantityPaid / SerumUtils.getBaseSplTokenMultiplier(baseDecimals)));
                }

                eventQueue.getEvents().add(tradeEvent);
            }
        }

        return eventQueue;
    }
}
