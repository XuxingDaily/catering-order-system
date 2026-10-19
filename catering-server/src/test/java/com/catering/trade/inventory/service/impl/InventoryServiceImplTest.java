package com.catering.trade.inventory.service.impl;

import com.catering.catalog.dish.mapper.DishMapper;
import com.catering.catalog.setmeal.mapper.SetmealMapper;
import com.catering.entity.OrderDetail;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.OrderBusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private DishMapper dishMapper;

    @Mock
    private SetmealMapper setmealMapper;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    void deductShouldAggregateSameItemsBeforeUpdatingStock() {
        List<OrderDetail> details = Arrays.asList(
                dishDetail(1L, 2),
                dishDetail(1L, 3),
                setmealDetail(10L, 2)
        );
        when(dishMapper.deductStock(1L, 5)).thenReturn(1);
        when(setmealMapper.deductStock(10L, 2)).thenReturn(1);

        inventoryService.deduct(details);

        verify(dishMapper).deductStock(1L, 5);
        verify(setmealMapper).deductStock(10L, 2);
    }

    @Test
    void deductShouldThrowBusinessExceptionWhenDishStockIsInsufficient() {
        List<OrderDetail> details = Arrays.asList(
                dishDetail(1L, 3),
                setmealDetail(10L, 1)
        );
        when(dishMapper.deductStock(1L, 3)).thenReturn(0);

        OrderBusinessException exception = assertThrows(
                OrderBusinessException.class,
                () -> inventoryService.deduct(details)
        );

        assertEquals(MessageConstant.DISH_STOCK_NOT_ENOUGH, exception.getMessage());
        verify(setmealMapper, never()).deductStock(10L, 1);
    }

    @Test
    void releaseShouldAggregateAndRestoreDishAndSetmealStock() {
        List<OrderDetail> details = Arrays.asList(
                dishDetail(2L, 1),
                dishDetail(2L, 2),
                setmealDetail(20L, 4)
        );
        when(dishMapper.releaseStock(2L, 3)).thenReturn(1);
        when(setmealMapper.releaseStock(20L, 4)).thenReturn(1);

        inventoryService.release(details);

        verify(dishMapper).releaseStock(2L, 3);
        verify(setmealMapper).releaseStock(20L, 4);
    }

    @Test
    void deductShouldRejectNonPositiveQuantityWithoutUpdatingDatabase() {
        List<OrderDetail> details = Collections.singletonList(dishDetail(1L, 0));

        OrderBusinessException exception = assertThrows(
                OrderBusinessException.class,
                () -> inventoryService.deduct(details)
        );

        assertEquals(MessageConstant.INVENTORY_QUANTITY_INVALID, exception.getMessage());
        verifyNoInteractions(dishMapper, setmealMapper);
    }

    @Test
    void deductShouldRejectDetailWithoutDishOrSetmeal() {
        OrderDetail detail = OrderDetail.builder().number(1).build();

        OrderBusinessException exception = assertThrows(
                OrderBusinessException.class,
                () -> inventoryService.deduct(Collections.singletonList(detail))
        );

        assertEquals(MessageConstant.INVENTORY_ITEM_INVALID, exception.getMessage());
        verifyNoInteractions(dishMapper, setmealMapper);
    }

    private OrderDetail dishDetail(Long dishId, Integer quantity) {
        return OrderDetail.builder()
                .dishId(dishId)
                .number(quantity)
                .build();
    }

    private OrderDetail setmealDetail(Long setmealId, Integer quantity) {
        return OrderDetail.builder()
                .setmealId(setmealId)
                .number(quantity)
                .build();
    }
}
