package ru.hilariousstartups.javaskills.psplayer.solution;

import org.springframework.stereotype.Component;
import ru.hilariousstartups.javaskills.psplayer.swagger_codegen.model.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
@Component
public class HireEmployees implements CurrentWorldRequestHandler{
    private CurrentWorldRequestHandler nextHandler;
    private final List<Employee> employees = new ArrayList<>();
    public HireEmployees() {

    }
    public HireEmployees(CurrentWorldRequestHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    public void setNextHandler(CurrentWorldRequestHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public void handle(CurrentWorldResponse currentWorldResponse, CurrentTickRequest request) {
        List<HireEmployeeCommand> hireEmployeeCommands = new ArrayList<>();
        List<FireEmployeeCommand> fireEmployeeCommands = new ArrayList<>();
        List<SetOffCheckoutLineCommand> setOffList = new ArrayList<>();
        Integer cnt = currentWorldResponse.getCurrentTick();
        if (cnt == 1) {
            //организуем набор кассиров берём сотню, чтобы повысить шансы взять опытных
            for (int i = 0; i < 100; i++ ) {
                HireEmployeeCommand hireEmployeeCommand = new HireEmployeeCommand();
                if (i%2 ==0) {
                    hireEmployeeCommand.setCheckoutLineId(1);
                } else {
                    hireEmployeeCommand.setCheckoutLineId(2);
                }
                //набирать ждунов пока лучшее решение
                hireEmployeeCommand.setExperience(HireEmployeeCommand.ExperienceEnum.SENIOR);
                hireEmployeeCommands.add(hireEmployeeCommand);
            }
        }
        if (cnt == 2) {
            //отбираем самых опытных из сотни
            for (int i = 0; i < 6; i++) {
                employees.add(currentWorldResponse.getEmployees().stream().filter(em -> !employees.contains(em)).max(Comparator.comparingInt(Employee::getExperience)).orElse(null));
            }
            //убираем текущих кассиров с касс
            currentWorldResponse.getCheckoutLines().forEach(line -> {
                SetOffCheckoutLineCommand setOffCommand = new SetOffCheckoutLineCommand();
                setOffCommand.setEmployeeId(line.getEmployeeId());
                setOffList.add(setOffCommand);
            });
            ArrayDeque<Integer> currentEmployers = employees.stream().map(Employee::getId).collect(Collectors.toCollection(ArrayDeque::new));
            currentWorldResponse.getEmployees().stream().filter(e -> !currentEmployers.contains(e.getId())).forEach(empl -> {
                FireEmployeeCommand fireEmployeeCommand = new FireEmployeeCommand();
                fireEmployeeCommand.setEmployeeId(empl.getId());
                fireEmployeeCommands.add(fireEmployeeCommand);
            });
        }
        request.setOffCheckoutLineCommands(setOffList);
        request.setHireEmployeeCommands(hireEmployeeCommands);
        request.setFireEmployeeCommands(fireEmployeeCommands);
        nextHandler.handle(currentWorldResponse, request);
    }
}
