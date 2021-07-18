package ch.openserum.serum.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bitcoinj.core.Utils;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.utils.ByteUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ch.openserum.serum.model.SerumUtils.U64_SIZE_BYTES;
import static ch.openserum.serum.model.SerumUtils.U128_SIZE_BYTES;

/**
 * Represents a Serum Open Orders account. Generally built from {@link SerumUtils}.
 */
@Getter
@Setter
@ToString
public class OpenOrdersAccount {

    @Getter
    @Setter
    @ToString
    public static class Order {
        private int orderIndex;
        private long clientId;
        private byte[] clientOrderId;
        private long price;
        private boolean isFreeSlot;
        private boolean isBid;
        private float floatPrice;
    }

    private static final int MARKET_OFFSET = 13;
    private static final int OWNER_OFFSET = MARKET_OFFSET + PublicKey.PUBLIC_KEY_LENGTH;
    private static final int BASE_TOKEN_FREE_OFFSET = OWNER_OFFSET + PublicKey.PUBLIC_KEY_LENGTH;
    private static final int BASE_TOKEN_TOTAL_OFFSET = BASE_TOKEN_FREE_OFFSET + U64_SIZE_BYTES;
    private static final int QUOTE_TOKEN_FREE_OFFSET = BASE_TOKEN_TOTAL_OFFSET + U64_SIZE_BYTES;
    private static final int QUOTE_TOKEN_TOTAL_OFFSET = QUOTE_TOKEN_FREE_OFFSET + U64_SIZE_BYTES;
    private static final int FREE_SLOT_BITS_OFFSET = QUOTE_TOKEN_TOTAL_OFFSET + U64_SIZE_BYTES;
    private static final int IS_BID_BITS_OFFSET = FREE_SLOT_BITS_OFFSET + U128_SIZE_BYTES;
    private static final int ORDERS_OFFSET = IS_BID_BITS_OFFSET + U128_SIZE_BYTES;
    private static final int CLIENT_IDS_OFFSET = ORDERS_OFFSET + 2048;

    private AccountFlags accountFlags;
    private PublicKey market;
    private PublicKey owner;
    private long baseTokenFree;
    private long baseTokenTotal;
    private long quoteTokenFree;
    private long quoteTokenTotal;
    private byte[] freeSlotBits;
    private byte[] isBidBits;
    private long referrerRebatesAccrued;

    // set manually
    private PublicKey ownPubkey;

    // deserialized
    private List<Long> longPrices;
    private List<Long> orderIds;
    private List<byte[]> clientOrderIds;
    private List<Boolean> freeSlots; // true if the index is free
    private List<Boolean> bidSlots; // true if the order is a bid
    private List<Order> orders;

    public OpenOrdersAccount() {
        this.longPrices = new ArrayList<>(128);
        this.orderIds = new ArrayList<>(128);
        this.clientOrderIds = new ArrayList<>(128);
        this.freeSlots = new ArrayList<>(128);
        this.bidSlots = new ArrayList<>(128);
        this.orders = new ArrayList<>(128);
    }

    public static OpenOrdersAccount readOpenOrdersAccount(byte[] data) {
        OpenOrdersAccount openOrdersAccount = new OpenOrdersAccount();

        final AccountFlags accountFlags = AccountFlags.readAccountFlags(data);
        openOrdersAccount.setAccountFlags(accountFlags);

        final PublicKey marketPubkey = PublicKey.readPubkey(data, MARKET_OFFSET);
        openOrdersAccount.setMarket(marketPubkey);

        final PublicKey ownerPubkey = PublicKey.readPubkey(data, OWNER_OFFSET);
        openOrdersAccount.setOwner(ownerPubkey);

        // baseTokenFree = unsettled balance
        final long baseTokenFree = Utils.readInt64(data, BASE_TOKEN_FREE_OFFSET);
        openOrdersAccount.setBaseTokenFree(baseTokenFree);

        final long baseTokenTotal = Utils.readInt64(data, BASE_TOKEN_TOTAL_OFFSET);
        openOrdersAccount.setBaseTokenTotal(baseTokenTotal);

        final long quoteTokenFree = Utils.readInt64(data, QUOTE_TOKEN_FREE_OFFSET);
        openOrdersAccount.setQuoteTokenFree(quoteTokenFree);

        final long quoteTokenTotal = Utils.readInt64(data, QUOTE_TOKEN_TOTAL_OFFSET);
        openOrdersAccount.setQuoteTokenTotal(quoteTokenTotal);

        byte[] freeSlotBits = Arrays.copyOfRange(data, FREE_SLOT_BITS_OFFSET, IS_BID_BITS_OFFSET);
        byte[] isBidBits = Arrays.copyOfRange(data, IS_BID_BITS_OFFSET, ORDERS_OFFSET);
        openOrdersAccount.setFreeSlotBits(freeSlotBits);
        openOrdersAccount.setIsBidBits(isBidBits);

        // orders = 128 * 16 = 2048 bytes of orders

        byte[] orders = Arrays.copyOfRange(data, ORDERS_OFFSET, CLIENT_IDS_OFFSET);
        byte[] clientIds = Arrays.copyOfRange(data, CLIENT_IDS_OFFSET, CLIENT_IDS_OFFSET + 1024);

        final List<Long> orderIds = new ArrayList<>();
        final List<Long> prices = new ArrayList<>();
        // ?
        final List<byte[]> clientOrderIds = new ArrayList<>();
        final List<Boolean> freeSlots = new ArrayList<>();
        final List<Boolean> bidSlots = new ArrayList<>();
        final List<Order> openOrdersAccountOrders = new ArrayList<>();

        for (int i = 0; i < 128; i++) {
            // read clientId
            long clientId = Utils.readInt64(clientIds, i * U64_SIZE_BYTES);
            byte[] clientOrderId = Arrays.copyOfRange(orders, i * U128_SIZE_BYTES, (i * U128_SIZE_BYTES) + U64_SIZE_BYTES);

            orderIds.add(clientId);
            clientOrderIds.add(clientOrderId);

            // read price
            long price = Utils.readInt64(orders, (i * U128_SIZE_BYTES) + U64_SIZE_BYTES);
            boolean isFreeSlot = ByteUtils.getBit(freeSlotBits, i) == 1;
            boolean isBid = ByteUtils.getBit(isBidBits, i) == 1;

            prices.add(price);
            freeSlots.add(isFreeSlot);
            bidSlots.add(isBid);

            if (!isFreeSlot) {
                Order order = new Order();
                order.setOrderIndex(i);
                order.setBid(isBid);
                order.setClientOrderId(clientOrderId);
                order.setPrice(price);
                order.setClientId(clientId);
                openOrdersAccountOrders.add(order);
            }
        }

        openOrdersAccount.setOrders(openOrdersAccountOrders);
        openOrdersAccount.setOrderIds(orderIds);
        openOrdersAccount.setLongPrices(prices);
        openOrdersAccount.setClientOrderIds(clientOrderIds);
        openOrdersAccount.setFreeSlots(freeSlots);
        openOrdersAccount.setBidSlots(bidSlots);

        return openOrdersAccount;
    }

}
