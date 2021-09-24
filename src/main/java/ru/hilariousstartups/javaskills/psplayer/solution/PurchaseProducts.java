package ru.hilariousstartups.javaskills.psplayer.solution;

import org.springframework.stereotype.Component;
import ru.hilariousstartups.javaskills.psplayer.swagger_codegen.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PurchaseProducts implements CurrentWorldRequestHandler {
    private CurrentWorldRequestHandler nextHandler;

    public void setNextHandler(CurrentWorldRequestHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public void handle(CurrentWorldResponse currentWorldResponse, CurrentTickRequest request) {
        Integer cnt = currentWorldResponse.getCurrentTick();
        if (cnt == 1) {
            ArrayList<BuyStockCommand> buyStockCommands = new ArrayList<>();
            request.setBuyStockCommands(buyStockCommands);
            ArrayList<PutOnRackCellCommand> putOnRackCellCommands = new ArrayList<>();
            request.setPutOnRackCellCommands(putOnRackCellCommands);
            //лист продуктов
            List<Product> stock = currentWorldResponse.getStock();
            //лист продуктовых полок где каждя полка хранит один вид товара
            List<RackCell> rackCells = currentWorldResponse.getRackCells();
            currentWorldResponse.getRackCells().stream().filter(rack -> rack.getProductId() == null || rack.getProductQuantity() != null/*.equals(0)*/)
                    .sorted(Comparator.comparingInt(RackCell::getVisibility).reversed()).forEach(rack -> {
                Product producttoPutOnRack = null;
                //если на текущая полка не закреплена за каким то товаром
                if (rack.getProductId() == null) {
                    //получаем поток полок, отбираем только те полки, на которых есть товар и преобразуем в лист товаров
                    List<Integer> productsOnRack = rackCells.stream().filter(r -> r.getProductId() != null).map(RackCell::getProductId).collect(Collectors.toList());
                    //добавляем в этот лист все товары из массива содержащего товары для выставления на полки
                    //в самом начале обхода эта коллекция пустая она постепенно наполняется продуктами которые
                    //мы ставим на полки в магазе и чтобы не было дублирования в некст шаге эти товары не берем
                    productsOnRack.addAll(putOnRackCellCommands.stream().map(c -> c.getProductId()).collect(Collectors.toList()));
                    //Товар для добавления на полку получаем так: из листа всех товаров на складе мы отбираем  самый первый
                    // который не содержится в данный момент на полках(лист выше)
                    producttoPutOnRack = stock.stream().filter(product -> !productsOnRack.contains(product.getId()))
                            .max(Comparator.comparingDouble(Product::getStockPrice)).orElse(null);
                }
                //если на текущая полка закреплена за товаром, но его количество == 0
                else {
                    //тогда находим этот товар на складе по productId, этот товар мы будем ещё докладывать на полку
                    producttoPutOnRack = stock.stream().filter(product -> product.getId().equals(rack.getProductId())).findFirst().orElse(null);
                }
                //получаем количество товара на текущей полке если товара нет ставим 0
                Integer productQuantity = rack.getProductQuantity();
                if (productQuantity == null) {
                    productQuantity = 0;
                }
                if (productQuantity == 0 && producttoPutOnRack.getInStock() == 0) {
                    // Вначале закупим товар на склад. Каждый ход закупать товар накладно, но ведь это тестовый игрок.
                    //количество товара для закупки = максимальное число на полке - текущее число на полке
                    // а мы закупим больше определенных товаров
                    Integer orderQuantity = rack.getCapacity() - productQuantity;
                    //если количество товара на складе < того что мы заказываем
                    //допустим 500 у нас максимум на складе теперь получим сумму заказа
                    Integer maxProductToBuy = 0;
                    switch (rack.getVisibility()) {
                        case 1:
                            if (producttoPutOnRack.getId() == 3
                                    || producttoPutOnRack.getId() == 4 || producttoPutOnRack.getId() == 6) {
                                maxProductToBuy = 4000;
                            } else {
                                maxProductToBuy = 3900;//3850;
                            }
                            break;
                        case 2:

                            maxProductToBuy = 4700;//4680;
                            break;
                        case 3:
                            if (producttoPutOnRack.getId() == 30) {
                                maxProductToBuy = 6500;
                            } else {
                                maxProductToBuy = 5600;//5460;
                            }
                            break;
                        case 4:
                            if (producttoPutOnRack.getId() == 21) {
                                maxProductToBuy = 12200;
                            } else {
                                maxProductToBuy = 12100;//11820;
                            }
                            break;
                        case 5:
                            if (producttoPutOnRack.getId() == 39) {
                                maxProductToBuy = 12500;
                            } else {
                                maxProductToBuy = 12200;//11820;
                            }
                            break;
                    }
                    Integer orderToStock = (maxProductToBuy - producttoPutOnRack.getInStock());
                    if (orderToStock <= maxProductToBuy) {
                        //продуктовый рейтинг
                        //значит мы можем его заказать, создаём команду для закупки
                        BuyStockCommand command = new BuyStockCommand();
                        command.setProductId(producttoPutOnRack.getId());
                        //command.setQuantity(100);
                        //закупаем то количество, что нам не хватает чтобы разместить на полке максимальное количество
                        command.setQuantity(orderToStock);
                        buyStockCommands.add(command);
                    }
                    // Далее разложим на полки. И сформируем цену. Накинем 10 рублей к оптовой цене
                    PutOnRackCellCommand command = new PutOnRackCellCommand();
                    //товар
                    command.setProductId(producttoPutOnRack.getId());
                    //текущая полка
                    command.setRackCellId(rack.getId());
                    //количество продукта что мы заказали
                    command.setProductQuantity(orderQuantity);
                    if (producttoPutOnRack.getSellPrice() == null) {
                        command.setSellPrice(producttoPutOnRack.getStockPrice() * 1.2);
                    }
                    //помещаем в массив команд заказ
                    putOnRackCellCommands.add(command);
                }
            });
        }
        nextHandler.handle(currentWorldResponse, request);
    }
}
