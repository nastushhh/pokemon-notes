import { useState } from "react";
import { format, startOfWeek } from "date-fns";
import { ru } from "date-fns/locale";
import { Container, Typography } from "@mui/material";
import Calendar from "../components/Calendar";

export default function MoodDiaryPage() {
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [currentWeekStart, setCurrentWeekStart] = useState(
    startOfWeek(new Date(), { locale: ru })
  );

  return (
    <Container
      maxWidth="md" // Увеличиваем максимальную ширину
      sx={{
        py: 6, // Увеличенные отступы
        px: 3,
        fontSize: "1.1rem", // Базовое увеличение шрифта
      }}
    >
      {/* Заголовок - увеличенный */}
      <Typography
        variant="h3"
        align="center"
        gutterBottom
        sx={{
          fontSize: { xs: "2rem", sm: "2.5rem" },
          mb: 4,
        }}
      >
        Дневник настроения
      </Typography>

      <Calendar
        selectedDate={selectedDate}
        onDateSelect={setSelectedDate}
        currentWeekStart={currentWeekStart}
        onChangeWeek={setCurrentWeekStart}
      />

      {/* Дата - увеличенная */}
      <Typography
        variant="h5"
        align="center"
        sx={{
          mt: 3,
          fontSize: "1.3rem",
          color: "text.secondary",
        }}
      >
        {format(selectedDate, "d MMMM yyyy", { locale: ru })}
      </Typography>
    </Container>
  );
}
