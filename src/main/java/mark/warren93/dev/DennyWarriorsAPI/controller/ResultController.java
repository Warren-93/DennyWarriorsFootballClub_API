package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.model.MatchResult;
import mark.warren93.dev.DennyWarriorsAPI.service.ResultService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/results")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping
    public List<MatchResult> getResults(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String competition,
            @RequestParam(required = false) String season) {
        return resultService.getResults(limit, competition, season);
    }

    @GetMapping("/{id}")
    public MatchResult getResultById(@PathVariable String id) {
        return resultService.getResultById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResult createResult(@RequestBody MatchResult result) {
        return resultService.createResult(result);
    }

    @PutMapping("/{id}")
    public MatchResult updateResult(@PathVariable String id, @RequestBody MatchResult result) {
        return resultService.updateResult(id, result);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResult(@PathVariable String id) {
        resultService.deleteResult(id);
    }
}
