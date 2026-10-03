package com.viajesito.route;

import java.time.LocalDate;
import java.util.Optional;

import com.viajesito.TestData;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RouteController.class)
@Import(RouteService.class)
class RouteControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RouteRepository repository;

	@Test
	void createsRouteNormalizingCodes() throws Exception {
		given(repository.save(any(Route.class))).willAnswer(inv -> {
			Route saved = inv.getArgument(0);
			saved.setId(5L);
			return saved;
		});
		String departure = LocalDate.now().plusDays(60).toString();

		mvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content("""
				{"origin": "eze", "destination": "bcn", "departureDate": "%s", "targetPriceMax": 950, "maxStops": 0}
				""".formatted(departure)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/routes/5"))
			.andExpect(jsonPath("$.maxStops").value(0))
			.andExpect(jsonPath("$.origin").value("EZE"))
			.andExpect(jsonPath("$.destination").value("BCN"))
			.andExpect(jsonPath("$.currency").value("USD"))
			.andExpect(jsonPath("$.adults").value(1))
			.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void rejectsInvalidRoute() throws Exception {
		mvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content("""
				{"origin": "EZEIZA", "destination": "MAD", "departureDate": "2027-02-10",
				 "returnDate": "2027-02-01", "targetPriceMin": 900, "targetPriceMax": 800}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.origin").exists())
			.andExpect(jsonPath("$.errors.returnAfterDeparture").exists())
			.andExpect(jsonPath("$.errors.targetRangeValid").exists());
	}

	@Test
	void pausesRoute() throws Exception {
		Route route = TestData.route();
		given(repository.findById(1L)).willReturn(Optional.of(route));
		given(repository.saveAndFlush(any(Route.class))).willAnswer(inv -> inv.getArgument(0));

		mvc.perform(post("/api/routes/1/pause"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void unknownRouteIs404() throws Exception {
		given(repository.findById(99L)).willReturn(Optional.empty());

		mvc.perform(get("/api/routes/99"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("No existe la ruta 99"));
	}

}
