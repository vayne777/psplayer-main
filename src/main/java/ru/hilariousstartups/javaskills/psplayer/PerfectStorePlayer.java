package ru.hilariousstartups.javaskills.psplayer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import ru.hilariousstartups.javaskills.psplayer.solution.CurrentWorldRequestHandler;
import ru.hilariousstartups.javaskills.psplayer.swagger_codegen.ApiClient;
import ru.hilariousstartups.javaskills.psplayer.swagger_codegen.ApiException;
import ru.hilariousstartups.javaskills.psplayer.swagger_codegen.api.PerfectStoreEndpointApi;
import ru.hilariousstartups.javaskills.psplayer.swagger_codegen.model.*;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PerfectStorePlayer implements ApplicationListener<ApplicationReadyEvent> {

    private String serverUrl;
    private Queue<Integer> restEmployees = new ArrayDeque<>();
    private Map<Integer,Integer> inHandling = new HashMap<>();
    private Map<Integer,Integer> currentWorkers = new HashMap<>();
    private Queue<CheckoutLine> lastEmployers = new ArrayDeque<>();
    private Map<Integer, Integer> productRateMap = new HashMap<>();
    private List<Map<Integer, Integer>> deltaMap = new ArrayList<>();
    private List<Integer> mostPopularProducts = new ArrayList<>() {{addAll(List.of(1,8,35,30,7,14,15,16,19,33,34,36,37,38,46));}};
    private Integer transport = 0;
    private List<Employee> employees = new ArrayList<>();
    private List<Integer> choco = new ArrayList<>() {{addAll(List.of(39,40,41));}};
    private List<Integer> milk = new ArrayList<>() {{addAll(List.of(8,9,10,11,12,13));}};
    private List<Integer> vegeatabeles = new ArrayList<>() {{addAll(List.of(3,4,5,6));}};
    private List<Integer> meat = new ArrayList<>() {{addAll(List.of(21,22,23));}};
    private List<Integer> cake = new ArrayList<>() {{addAll(List.of(30,28,29));}};

    @Autowired
    List<CurrentWorldRequestHandler> handlers;

    public PerfectStorePlayer(@Value("${rs.endpoint:http://localhost:9080}") String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(serverUrl);

        PerfectStoreEndpointApi psApiClient = new PerfectStoreEndpointApi(apiClient);

        log.info("Игрок готов. Подключаемся к серверу..");
        awaitServer(psApiClient);

        log.info("Подключение к серверу успешно. Начинаем игру");
        try {
            CurrentWorldResponse currentWorldResponse = null;
            Integer cnt = 0;
            do {
                cnt += 1;
                if (cnt % 360 == 0) {
                    log.info("Пройден " + cnt + " тик");
                }
                if (currentWorldResponse == null) {
                    currentWorldResponse = psApiClient.loadWorld();
                }
                CurrentTickRequest request = new CurrentTickRequest();

                // Смотрим на каких кассах нет кассира (либо не был назначен, либо ушел с кассы отдыхать), нанимаем новых кассиров и ставим на эти кассы.
                // Нанимаем самых опытных!
                //каждый тик проходим по кассам
                //снизит цены если количество товара на складе больше чем продаём в среднем за 100 тиков
                currentWorldResponse = psApiClient.tick(request);
            }
            while (!currentWorldResponse.isGameOver());

            // Если пришел Game Over, значит все время игры закончилось. Пора считать прибыль

            log.info("продуктовый рейтинг мапа: " + productRateMap.toString());
            log.info("Я заработал " + (currentWorldResponse.getIncome() - currentWorldResponse.getSalaryCosts() - currentWorldResponse.getStockCosts()) + "руб. \n" +
                    "транспортные расходы составили: " + transport);

        } catch (ApiException e) {
            log.error(e.getMessage(), e);
        }

    }

    private void awaitServer(PerfectStoreEndpointApi psApiClient) {
        int awaitTimes = 60;
        int cnt = 0;
        boolean serverReady = false;
        do {
            try {
                cnt += 1;
                psApiClient.loadWorld();
                serverReady = true;

            } catch (ApiException e) {
                try {
                    Thread.currentThread().sleep(1000L);
                } catch (InterruptedException interruptedException) {
                    e.printStackTrace();
                }
            }
        } while (!serverReady && cnt < awaitTimes);
    }


}
