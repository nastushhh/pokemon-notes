import {
  format,
  startOfWeek,
  addDays,
  isSameDay,
  subWeeks,
  addWeeks,
} from "date-fns";
import { Box, Paper, Typography, IconButton } from "@mui/material";
import { ChevronLeft, ChevronRight } from "@mui/icons-material";
import { ru } from "date-fns/locale";

export default function Calendar({
  selectedDate,
  onDateSelect,
  currentWeekStart,
  onChangeWeek,
}) {
  const today = new Date();

  // Увеличенные размеры для всех устройств
  const daySize = 64; // +33% к предыдущему размеру
  const dayFontSize = "1.5rem"; // Крупный текст дат
  const dayOfWeekFontSize = "1.4rem"; // Увеличенные дни недели

  const weekDays = Array.from({ length: 7 }, (_, i) => {
    const day = addDays(currentWeekStart, i);
    return {
      date: day,
      dayOfMonth: format(day, "d", { locale: ru }),
      dayOfWeek: format(day, "EEE", { locale: ru }),
    };
  });

  return (
    <Box sx={{ mb: 4, width: "100%" }}>
      {/* Кнопки переключения недель - увеличенные */}
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          mb: 3, // Увеличенный отступ
        }}
      >
        <IconButton
          size="large"
          onClick={() => onChangeWeek(subWeeks(currentWeekStart, 1))}
          sx={{ fontSize: "2rem" }}
        >
          <ChevronLeft fontSize="large" />
        </IconButton>

        <Typography variant="h6" sx={{ fontSize: "1.5rem" }}>
          {format(currentWeekStart, "MMMM yyyy", { locale: ru })}
        </Typography>

        <IconButton
          size="large"
          onClick={() => onChangeWeek(addWeeks(currentWeekStart, 1))}
          sx={{ fontSize: "2rem" }}
        >
          <ChevronRight fontSize="large" />
        </IconButton>
      </Box>

      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          gap: 3,
        }}
      >
        {weekDays.map((day) => (
          <Paper
            key={day.date.toString()}
            elevation={3}
            sx={{
              p: 2, // Увеличенный padding
              minWidth: daySize,
              height: daySize,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              cursor: "pointer",
              backgroundColor: isSameDay(day.date, today)
                ? "#ffebee"
                : isSameDay(day.date, selectedDate)
                ? "#e0e0e0"
                : "#f5f5f5",
              "&:hover": { backgroundColor: "#eeeeee" },
              flex: 1, // Равномерное распределение
            }}
            onClick={() => onDateSelect(day.date)}
          >
            <Typography variant="caption" sx={{ fontSize: dayOfWeekFontSize }}>
              {day.dayOfWeek}
            </Typography>
            <Typography variant="h6" sx={{ fontSize: dayFontSize }}>
              {day.dayOfMonth}
            </Typography>
          </Paper>
        ))}
      </Box>
    </Box>
  );
}
