package ru.hilariousstartups.javaskills.psplayer.solution;

import org.springframework.stereotype.Component;
import ru.hilariousstartups.javaskills.psplayer.swagger_codegen.model.*;

import java.util.*;
import java.util.stream.Collectors;
@Component
public class ChangingEmployees implements CurrentWorldRequestHandler {
    private CurrentWorldRequestHandler nextHandler;

    public void setNextHandler(CurrentWorldRequestHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    private Map<Integer,Integer> currentWorkers = new HashMap<>();
    private Queue<Integer> restEmployees = new ArrayDeque<>();
    @Override
    public void handle(CurrentWorldResponse currentWorldResponse, CurrentTickRequest request) {
        Integer cnt = currentWorldResponse.getCurrentTick();
        List<SetOnCheckoutLineCommand> setOnList = new ArrayList<>();
        List<Employee> employees = currentWorldResponse.getEmployees();
        //все 6 кассиров уже наняты и ждут
        //на 3м тике отправляем первую смену
        if (cnt==3) {
            ArrayDeque<Integer> currentEmployers = employees.stream().map(Employee::getId).collect(Collectors.toCollection(ArrayDeque::new));
            Integer finalCnt2 = cnt;
            currentWorldResponse.getCheckoutLines().forEach(line -> {
                SetOnCheckoutLineCommand setCommand = new SetOnCheckoutLineCommand();
                Integer emplId = currentEmployers.poll();
                setCommand.setEmployeeId(emplId);
                setCommand.setCheckoutLineId(line.getId());
                setOnList.add(setCommand);
                //отправляем на кассу записывавем время окончания работы
                currentWorkers.put(line.getId(), (finalCnt2 + 479));
            });
            restEmployees.addAll(currentEmployers);
        }
            // log.info("ЩАС НА КАССАХ: " + currentWorldResponse.getCheckoutLines().toString());
            currentWorldResponse.getCheckoutLines().forEach(line -> {
                //чекаем мапу работников которые щас за кассами
                //если в мапе есть такая касса
                if (!currentWorkers.isEmpty() && currentWorkers.containsKey(line.getId())) {
                        //получаем время окончания смены
                        int expirationTime = currentWorkers.get(line.getId());
                        //если пора меняться проверим на готовность к закрытию
                        if (cnt == expirationTime) {
                            //если на кассе нет посетителей тогда меняем кассира на нового
                            if (line.getEmployeeId() == null && line.getCustomerId() == null) {
                                if (restEmployees.size() > 4) {
                                    //№ кассира
                                    int id = restEmployees.poll();
                                    currentWorkers.put(line.getId(), cnt + 479);
                                    SetOnCheckoutLineCommand setCommand = new SetOnCheckoutLineCommand();
                                    setCommand.setEmployeeId(id);
                                    setCommand.setCheckoutLineId(line.getId());
                                    setOnList.add(setCommand);
                                }
                            } else if (line.getCustomerId() == null && line.getEmployeeId() != null) {
                                //проверить есть ли отдыхающие кассиры
                                //если нет просто нанимаем нового
                                int id = line.getEmployeeId();
                                //значит на след шаге Employer будет null и мы сможем нанять некст
                                currentWorkers.put(line.getId(), cnt + 1);
                                restEmployees.add(id);
                            } else {
                                //если на кассе есть покупатель ждём конец обработки
                                int customerId = line.getCustomerId();
                                int oldExpTime = currentWorkers.get(line.getId());
                                currentWorkers.replace(line.getId(), oldExpTime + 1);
                            }
                    }
                }//мапа пустая значит нанимаем челов
            });

        request.setOnCheckoutLineCommands(setOnList);
        nextHandler.handle(currentWorldResponse, request);
    }
}
