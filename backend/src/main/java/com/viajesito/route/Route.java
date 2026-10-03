package com.viajesito.route;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "routes")
public class Route {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 3)
	private String origin;

	@Column(nullable = false, length = 3)
	private String destination;

	@Column(nullable = false)
	private LocalDate departureDate;

	private LocalDate returnDate;

	@Column(nullable = false)
	private int adults = 1;

	/** Máximo de escalas aceptadas: null = cualquiera, 0 = solo directos. */
	private Integer maxStops;

	@Column(nullable = false, length = 3)
	private String currency = "USD";

	@Column(precision = 10, scale = 2)
	private BigDecimal targetPriceMin;

	@Column(precision = 10, scale = 2)
	private BigDecimal targetPriceMax;

	@Column(nullable = false)
	private boolean active = true;

	private String notes;

	@Column(nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	@PrePersist
	void onCreate() {
		createdAt = OffsetDateTime.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public LocalDate getDepartureDate() {
		return departureDate;
	}

	public void setDepartureDate(LocalDate departureDate) {
		this.departureDate = departureDate;
	}

	public LocalDate getReturnDate() {
		return returnDate;
	}

	public void setReturnDate(LocalDate returnDate) {
		this.returnDate = returnDate;
	}

	public int getAdults() {
		return adults;
	}

	public void setAdults(int adults) {
		this.adults = adults;
	}

	public Integer getMaxStops() {
		return maxStops;
	}

	public void setMaxStops(Integer maxStops) {
		this.maxStops = maxStops;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public BigDecimal getTargetPriceMin() {
		return targetPriceMin;
	}

	public void setTargetPriceMin(BigDecimal targetPriceMin) {
		this.targetPriceMin = targetPriceMin;
	}

	public BigDecimal getTargetPriceMax() {
		return targetPriceMax;
	}

	public void setTargetPriceMax(BigDecimal targetPriceMax) {
		this.targetPriceMax = targetPriceMax;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

}
