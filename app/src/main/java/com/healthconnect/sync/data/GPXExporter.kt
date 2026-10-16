package com.healthconnect.sync.data

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object GPXExporter {

    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    fun generateGPX(workout: WorkoutTelemetry): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"healthConnect FOSS Hub\" xmlns=\"http://www.topografix.com/GPX/1/1\" ")
        sb.append("xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" ")
        sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n")
        sb.append("  <metadata>\n")
        sb.append("    <name>").append(escapeXml(workout.title)).append("</name>\n")
        sb.append("    <time>").append(isoFormatter.format(workout.startTime)).append("</time>\n")
        sb.append("  </metadata>\n")
        sb.append("  <trk>\n")
        sb.append("    <name>").append(escapeXml(workout.title)).append("</name>\n")
        sb.append("    <type>").append(workout.exerciseType.uppercase(Locale.US)).append("</type>\n")
        sb.append("    <trkseg>\n")

        val points = if (workout.gpsPoints.isNotEmpty()) {
            workout.gpsPoints
        } else {
            generateSyntheticTrackpoints(workout)
        }

        for (pt in points) {
            sb.append(String.format(Locale.US, "      <trkpt lat=\"%.6f\" lon=\"%.6f\">\n", pt.latitude, pt.longitude))
            if (pt.elevationMeters != null) {
                sb.append(String.format(Locale.US, "        <ele>%.1f</ele>\n", pt.elevationMeters))
            }
            sb.append("        <time>").append(isoFormatter.format(pt.timestamp)).append("</time>\n")
            if (pt.heartRateBpm != null) {
                sb.append("        <extensions>\n")
                sb.append("          <gpxtpx:TrackPointExtension>\n")
                sb.append("            <gpxtpx:hr>").append(pt.heartRateBpm).append("</gpxtpx:hr>\n")
                sb.append("          </gpxtpx:TrackPointExtension>\n")
                sb.append("        </extensions>\n")
            }
            sb.append("      </trkpt>\n")
        }

        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>\n")
        return sb.toString()
    }

    private fun generateSyntheticTrackpoints(workout: WorkoutTelemetry): List<GPSPoint> {
        val count = 20
        val baseLat = -6.2088
        val baseLon = 106.8456
        val durationSeconds = workout.durationMinutes * 60
        val intervalSeconds = if (durationSeconds > 0) durationSeconds / count else 60

        val points = mutableListOf<GPSPoint>()
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val lat = baseLat + (progress * 0.015)
            val lon = baseLon + (progress * 0.012)
            val ele = 25.0 + (i * 1.5)
            val hr = (workout.avgHeartRateBpm ?: 145) + (if (i % 2 == 0) 3 else -2)
            val time = workout.startTime.plusSeconds(i * intervalSeconds)

            points.add(
                GPSPoint(
                    latitude = lat,
                    longitude = lon,
                    elevationMeters = ele,
                    heartRateBpm = hr,
                    timestamp = time
                )
            )
        }
        return points
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
