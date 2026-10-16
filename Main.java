public class Main {
    public static void main(String[] args) {
        QuizManager manager = new QuizManager();
        Teacher teacher = new Teacher("T1", "Ajay", "ajay@example.com", manager);

        Quiz quiz = teacher.createQuiz("Java OOP Quiz", 10);
        System.out.println("Quiz created. Class code: " + quiz.getClassCode());
        System.out.println("Questions added: " + quiz.getQuestionCount());
    }
}