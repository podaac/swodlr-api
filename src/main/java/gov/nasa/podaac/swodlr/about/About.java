package gov.nasa.podaac.swodlr.about;

public record About(
    String version,
    long uptime,
    long currentTime
) { }
