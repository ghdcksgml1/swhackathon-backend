package com.proceed.swhackathon.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.proceed.swhackathon.exception.order.OrderNotFoundException;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserOrderDetail extends TimeZone {

    @Id @GeneratedValue
    @Column(name = "userOrderDetail_id")
    private Long id;

    private int totalPrice;

     // DONE: 결제완료(리뷰는 작성하지 않은 상태), CANCEL: 결제취소, REVIEW_DONE: 리뷰까지 완료
    private String userOrderDetailStatus = "DONE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @OneToMany(mappedBy = "userOrderDetail", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails = new ArrayList<>();

    public void calTotalPrice(){
        totalPrice = 0;
        for(OrderDetail od : orderDetails) {
            if(od.isMenuCheck())
                totalPrice += od.getTotalPrice();
        }
        order.calCurrentAmount(totalPrice);
    }

    public void changeTotalPrice(boolean trigger, int amount){
        if(trigger){
            totalPrice += amount;
        }else{
            totalPrice -= amount;
        }
    }

    public void cancel(){
        if(order == null){
            throw new OrderNotFoundException();
        }
        orderDetails.stream().forEach(od -> od.setUserOrderDetail(null));
        orderDetails = null;
        order.cancel(totalPrice, this);
    }
}
