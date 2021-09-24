package ru.hilariousstartups.javaskills.psplayer.solution;

import org.springframework.stereotype.Component;
import ru.hilariousstartups.javaskills.psplayer.swagger_codegen.model.CurrentTickRequest;
import ru.hilariousstartups.javaskills.psplayer.swagger_codegen.model.CurrentWorldResponse;
import ru.hilariousstartups.javaskills.psplayer.swagger_codegen.model.Product;
import ru.hilariousstartups.javaskills.psplayer.swagger_codegen.model.PutOnRackCellCommand;

import java.util.ArrayList;
import java.util.List;
@Component
public class SupplementRacks implements CurrentWorldRequestHandler {
    private CurrentWorldRequestHandler nextHandler;

    public void setHandler(CurrentWorldRequestHandler handler) {
        this.nextHandler = handler;
    }

    @Override
    public void handle(CurrentWorldResponse currentWorldResponse, CurrentTickRequest request) {
        List<PutOnRackCellCommand> putOnRackCellCommands = new ArrayList<>();
        List<Product> stock = currentWorldResponse.getStock();
        Integer cnt = currentWorldResponse.getCurrentTick();
        currentWorldResponse.getRackCells().stream().forEach(rack -> {
            //если на полке нет товаров
            if (rack.getProductId() != null && rack.getProductQuantity() == 0) {
                //тогда берём этот товар со склада
                Product producttoPutOnRack = stock.stream().filter(product -> product.getId().equals(rack.getProductId())).findFirst().orElse(null);
                if (producttoPutOnRack.getInStock() > 0) {
                    // log.info("ALERT ALERT ТИК " + finalCnt1 + " ТОВАРОВ С АЙДИ " + rack.getProductId() + " НЕТ НА СКЛАДЕ");
                    PutOnRackCellCommand command = new PutOnRackCellCommand();
                    command.setProductId(producttoPutOnRack.getId());
                    command.setRackCellId(rack.getId());
                    //берём со склада 20 продуктов и ставим на полку
                    Integer orderQuantity = rack.getCapacity() - rack.getProductQuantity();
                    command.setProductQuantity(orderQuantity);
                    if (cnt > 9999 && producttoPutOnRack.getInStock() > 100) {
                        if (producttoPutOnRack.getSellPrice() == null) {
                            command.setSellPrice(producttoPutOnRack.getStockPrice()*0.9);
                        }
                    } else
                    if (producttoPutOnRack.getSellPrice() == null) {
                        command.setSellPrice(producttoPutOnRack.getStockPrice() * 1.2);
                    }
                    putOnRackCellCommands.add(command);
                }
            }
        });
        nextHandler.handle(currentWorldResponse,request);
    }
}
