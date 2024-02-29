package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.responses.ErrorResponse;
import searchengine.dto.responses.Response;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    private final SearchService searchService;


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        if (indexingService.indexingAll()) {
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        }
        return new ResponseEntity<>(new ErrorResponse(false, "Индексация уже запущена"), HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        if (indexingService.stopIndexing()) {
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new ErrorResponse(false, "Индексация не запущена"), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam(name = "url") String url) {
        if (url.isEmpty()) {
            return new ResponseEntity<>(new ErrorResponse(false, "Ошибка ввода пустой страницы"), HttpStatus.BAD_REQUEST);
        } else {
            if (indexingService.urlIndexing(url)) {
                log.info("Страница - " + url + " - добавлена для индексации");
                return new ResponseEntity<>(new Response(true), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new ErrorResponse(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"),
                        HttpStatus.BAD_REQUEST);
            }
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam(name = "query", required = false, defaultValue = "") String request,
                                         @RequestParam(name = "site", required = false, defaultValue = "") String site,
                                         @RequestParam(name = "offset", required = false, defaultValue = "0") int offset,
                                         @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) {
        if (request.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, "Задан пустой поисковый запрос"));
        } else if (!site.isEmpty() && siteRepository.findByUrl(site) == null) {
            return new ResponseEntity<>(new ErrorResponse(false, "Указанная страница не найдена"),
                            HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(searchService.getSearchResult(request, site, offset, limit), HttpStatus.OK);

    }
}
