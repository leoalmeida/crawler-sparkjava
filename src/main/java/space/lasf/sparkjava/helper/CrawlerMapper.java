package space.lasf.sparkjava.helper;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import space.lasf.sparkjava.dto.CrawlerDto;
import space.lasf.sparkjava.entity.Crawler;

/**
 * A utility class for mapping {@link Crawler} domain objects to {@link CrawlerDto}
 * data transfer objects. This class cannot be instantiated.
 */
public final class CrawlerMapper {

	/**
	 * Private constructor to prevent instantiation of this utility class.
	 */
	private CrawlerMapper() {
		throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	/**
	 * Converts a single {@link Crawler} domain object into a {@link CrawlerDto}.
	 *
	 * @param crawler The {@code Crawler} object to convert. Can be {@code null}.
	 * @return A new {@code CrawlerDto} instance, or {@code null} if the input crawler is {@code null}.
	 */
	public static CrawlerDto toCrawlerDto(Crawler crawler) {
		if (crawler == null) {
			return null;
		}

		CrawlerDto dto = new CrawlerDto();
		dto.setId(crawler.getId());
		dto.setStatus(crawler.getStatus().name().toLowerCase());
		dto.setUrls(new ArrayList<>(crawler.getUrls()));
		return dto;
	}

	/**
	 * Converts a list of {@link Crawler} domain objects into a list of {@link CrawlerDto}s.
	 *
	 * @param crawlerList The list of {@code Crawler} objects to convert. Can be {@code null}.
	 * @return A new list of {@code CrawlerDto}s, or an empty list if the input is {@code null} or empty.
	 */
	public static List<CrawlerDto> toCrawlerDtoList(List<Crawler> crawlerList) {
		if (crawlerList == null || crawlerList.isEmpty()) {
			return Collections.emptyList();
		}
		return crawlerList.stream().map(CrawlerMapper::toCrawlerDto).collect(Collectors.toList());
	}
}