package cn.momia.jobs.order;

import cn.momia.common.service.AbstractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.util.ArrayList;
import java.util.List;

public class OrderCleaner extends AbstractService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCleaner.class);

    public void run() {
        try {
            LOGGER.info("start to clean orders ...");

            List<Order> expiredOrders = getExpiredOrders();
            List<Order> removedOrders = removeOrders(expiredOrders);
            unlockStocks(removedOrders);

            cleanActivityOrders();

            LOGGER.info("clean orders finished");
        } catch (Exception e) {
            LOGGER.error("fail to clean expired orders", e);
        }
    }

    private List<Order> getExpiredOrders() {
        String sql = "SELECT A.Id, A.UserId, B.Id AS SubjectId, B.Type AS SubjectType, COUNT(A.Id) AS Count FROM SG_SubjectOrder A INNER JOIN SG_Subject B ON A.SubjectId=B.Id INNER JOIN SG_SubjectOrderPackage C ON A.Id=C.OrderId WHERE (A.Status=1 OR A.Status=2) AND DATE_ADD(A.UpdateTime, INTERVAL 10 MINUTE)<NOW() GROUP BY A.Id";
        List<Order> orders = queryObjectList(sql, null, Order.class);

        LOGGER.info("get {} expired orders", orders.size());

        return orders;
    }

    private List<Order> removeOrders(List<Order> expiredOrders) {
        List<Order> removedOrders = new ArrayList<Order>();

        for (Order order : expiredOrders) {
            if (removeOrder(order.getId())) removedOrders.add(order);
        }

        LOGGER.info("{} orders are removed", removedOrders.size());

        return removedOrders;
    }

    public boolean removeOrder(long orderId) {
        try {
            String sql = "UPDATE SG_SubjectOrder SET Status=0 WHERE Id=? AND (Status=1 OR status=2)";
            return update(sql, new Object[] { orderId });
        } catch (Exception e) {
            LOGGER.error("fail to remove order: {}", orderId, e);
            return false;
        }
    }

    private void unlockStocks(List<Order> removedOrders) {
        for (Order order : removedOrders) {
            if (order.getSubjectType() == 2) unlockStock(order);
        }
    }

    private void unlockStock(Order order) {
        try {
            String sql = "UPDATE SG_Subject SET Stock=Stock+? WHERE Id=? AND Status=1";
            update(sql, new Object[] { order.getCount(), order.getSubjectId() });
        } catch (Exception e) {
            LOGGER.error("fail to unlock stock for order: {}", order.getId(), e);
        }
    }


    private void cleanActivityOrders() {
        List<Long> expiredEntryIds = getExpiredEntryIds();
        for (final long entryId : expiredEntryIds) {
            try {
                execute(new TransactionCallback() {
                    @Override
                    public Object doInTransaction(TransactionStatus status) {
                        String sql = "UPDATE SG_ActivityEntry SET Status=0 WHERE Id=? AND (Status=1 OR Status=2)";
                        if (update(sql, new Object[] { entryId })) {
                            sql = "SELECT ActivityId FROM SG_ActivityEntry WHERE Id=?";
                            int activityId = queryInt(sql, new Object[] { entryId });
                            sql = "UPDATE SG_Activity SET UnlockedStock=UnlockedStock+1, LockedStock=LockedStock-1 WHERE Id=? AND UnlockedStock<Stock AND LockedStock>0";
                            update(sql, new Object[] { activityId });
                        }

                        return null;
                    }
                });
            } catch (Exception e) {
                LOGGER.error("exception, entryId: {}", entryId, e);
            }
        }
    }

    private List<Long> getExpiredEntryIds() {
        String sql = "SELECT A.Id FROM SG_ActivityEntry A INNER JOIN SG_Activity B ON A.ActivityId=B.Id WHERE A.Status>0 AND A.Status<3 AND DATE_ADD(A.UpdateTime, INTERVAL 10 MINUTE)<NOW() AND B.Stock>0";
        return queryLongList(sql);
    }
}
