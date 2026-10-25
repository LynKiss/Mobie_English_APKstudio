package com.example.myapplication

import androidx.compose.ui.graphics.Color

internal data class Word(
    val english: String,
    val pronunciation: String,
    val vietnamese: String,
    val example: String,
    val imageCue: String,
    val englishDefinition: String = ""
) {
    val id: String get() = english.trim().lowercase()
}

internal data class Lesson(
    val stage: Int,
    val category: String,
    val title: String,
    val subtitle: String,
    val duration: String,
    val icon: String,
    val color: Color,
    val vocabulary: List<Word>
)

internal data class CourseStage(val number: Int, val title: String, val description: String)

internal val courseStages = listOf(
    CourseStage(1, "Nền tảng giao tiếp", "Chào hỏi, thói quen và những câu nói đầu tiên"),
    CourseStage(2, "Tiếng Anh quanh ta", "Đi lại, ăn uống và khám phá thế giới"),
    CourseStage(3, "Tự tin mỗi ngày", "Công việc, cảm xúc và những cuộc trò chuyện tự nhiên"),
    CourseStage(4, "Du lịch tự tin", "Sân bay, khách sạn và những chuyến đi thuận lợi"),
    CourseStage(5, "Sống bằng tiếng Anh", "Sức khỏe, nhà cửa, kế hoạch và phát triển bản thân")
)

internal val lessons = listOf(
    Lesson(1, "GIAO TIẾP", "Làm quen & chào hỏi", "Tự tin bắt đầu một cuộc trò chuyện", "8 phút", "☀", Color(0xFFE2F1E9), listOf(
        Word("Nice to meet you", "/naɪs tə miːt juː/", "Rất vui được gặp bạn", "Nice to meet you, Anna!", "👋"),
        Word("How are you?", "/haʊ ɑːr juː/", "Bạn khỏe không?", "Hi, Tom. How are you?", "🙂"),
        Word("See you later", "/siː juː ˈleɪtər/", "Hẹn gặp lại sau", "See you later, have a great day!", "👋"),
        Word("I'm from Vietnam", "/aɪm frəm ˌviːətˈnɑːm/", "Mình đến từ Việt Nam", "I'm from Vietnam. Where are you from?", "🌏")
    )),
    Lesson(1, "TỪ VỰNG", "Một ngày của bạn", "Những từ quen thuộc, dùng mỗi ngày", "10 phút", "◷", Color(0xFFE6EFF4), listOf(
        Word("Wake up", "/weɪk ʌp/", "Thức dậy", "I wake up at seven.", "⏰"),
        Word("Commute", "/kəˈmjuːt/", "Đi làm / đi học", "I commute by bus.", "🚌"),
        Word("Take a break", "/teɪk ə breɪk/", "Nghỉ giải lao", "Let's take a short break.", "☕"),
        Word("Unwind", "/ʌnˈwaɪnd/", "Thư giãn", "I unwind with a good book.", "📖")
    )),
    Lesson(1, "NGỮ PHÁP", "Thì hiện tại đơn", "Nói về thói quen và sự thật", "12 phút", "✳", Color(0xFFFFEEE3), listOf(
        Word("Every day", "/ˈevri deɪ/", "Mỗi ngày", "She walks every day.", "📅"),
        Word("Usually", "/ˈjuːʒuəli/", "Thường xuyên", "I usually have coffee in the morning.", "☕"),
        Word("Sometimes", "/ˈsʌmtaɪmz/", "Đôi khi", "We sometimes study together.", "🔁"),
        Word("On weekends", "/ɑːn ˈwiːkˌendz/", "Vào cuối tuần", "They relax on weekends.", "🌤")
    )),
    Lesson(2, "DU LỊCH", "Hỏi đường & di chuyển", "Tìm đường dễ dàng ở một thành phố mới", "10 phút", "⌖", Color(0xFFE8EEF9), listOf(
        Word("Turn left", "/tɜːrn left/", "Rẽ trái", "Turn left at the next street.", "⬅️"),
        Word("Go straight", "/ɡoʊ streɪt/", "Đi thẳng", "Go straight for two blocks.", "⬆️"),
        Word("How far?", "/haʊ fɑːr/", "Bao xa?", "How far is the station?", "📍"),
        Word("Cross the street", "/krɔːs ðə striːt/", "Băng qua đường", "Cross the street at the lights.", "🚦")
    )),
    Lesson(2, "TỪ VỰNG", "Bốn hướng la bàn", "Mô tả phương hướng trên bản đồ và trong đời sống", "7 phút", "⌖", Color(0xFFE8EEF9), listOf(
        Word("North", "/nɔːrθ/", "Phương bắc, hướng bắc, phía bắc", "The wind came from the north today.", "⬆️", "One of the four main directions, opposite to south. On a map, it is usually at the top."),
        Word("South", "/saʊθ/", "Phương nam, hướng nam, phía nam", "We drove south toward the coast.", "⬇️", "The direction opposite to north; on most maps, it is at the bottom."),
        Word("East", "/iːst/", "Phương đông, hướng đông, phía đông", "The sun rises in the east.", "🌅", "The direction where the sun rises, opposite to west."),
        Word("West", "/west/", "Phương tây, hướng tây, phía tây", "The sun sets in the west.", "🌇", "The direction where the sun sets, opposite to east.")
    )),
    Lesson(2, "ẨM THỰC", "Gọi món ở nhà hàng", "Gọi món lịch sự và hỏi về thực đơn", "9 phút", "♨", Color(0xFFFFEFE4), listOf(
        Word("The menu, please", "/ðə ˈmenjuː pliːz/", "Cho tôi xin thực đơn", "Could I see the menu, please?", "📖"),
        Word("I'd like...", "/aɪd laɪk/", "Tôi muốn gọi...", "I'd like a cup of tea.", "🍵"),
        Word("Delicious", "/dɪˈlɪʃəs/", "Ngon", "This soup is delicious.", "😋"),
        Word("The bill, please", "/ðə bɪl pliːz/", "Cho tôi xin hóa đơn", "Could we have the bill, please?", "🧾")
    )),
    Lesson(2, "GIAO TIẾP", "Mua sắm & thanh toán", "Hỏi giá, chọn đồ và thanh toán", "11 phút", "◇", Color(0xFFF9EEDC), listOf(
        Word("How much is it?", "/haʊ mʌtʃ ɪz ɪt/", "Món này giá bao nhiêu?", "How much is it in total?", "🏷️"),
        Word("I'm just looking", "/aɪm dʒʌst ˈlʊkɪŋ/", "Tôi chỉ xem thôi", "Thanks, I'm just looking.", "👀"),
        Word("Can I try it on?", "/kæn aɪ traɪ ɪt ɑːn/", "Tôi thử được không?", "Can I try this jacket on?", "👕"),
        Word("Pay by card", "/peɪ baɪ kɑːrd/", "Thanh toán bằng thẻ", "Can I pay by card?", "💳")
    )),
    Lesson(3, "CÔNG VIỆC", "Trao đổi nơi công sở", "Diễn đạt ý kiến trong cuộc họp", "12 phút", "▤", Color(0xFFE8F1EF), listOf(
        Word("In my opinion", "/ɪn maɪ əˈpɪnjən/", "Theo ý kiến của tôi", "In my opinion, this is a good plan.", "💬"),
        Word("I agree", "/aɪ əˈɡriː/", "Tôi đồng ý", "I agree with your suggestion.", "🤝"),
        Word("Could you clarify?", "/kʊd juː ˈklærɪfaɪ/", "Bạn có thể giải thích rõ hơn?", "Could you clarify the last point?", "❓"),
        Word("Follow up", "/ˈfɑːloʊ ʌp/", "Theo dõi / trao đổi tiếp", "I'll follow up by email.", "📧")
    )),
    Lesson(3, "CẢM XÚC", "Nói về cảm xúc", "Chia sẻ cảm xúc và phản hồi tự nhiên", "8 phút", "♡", Color(0xFFFCE9E8), listOf(
        Word("I'm excited", "/aɪm ɪkˈsaɪtɪd/", "Mình rất hào hứng", "I'm excited about the trip.", "🤩"),
        Word("A bit nervous", "/ə bɪt ˈnɜːrvəs/", "Hơi lo lắng", "I'm a bit nervous about the test.", "😅"),
        Word("That sounds great", "/ðæt saʊndz ɡreɪt/", "Nghe tuyệt đấy", "A picnic? That sounds great!", "✨"),
        Word("Don't worry", "/doʊnt ˈwɜːri/", "Đừng lo", "Don't worry, I can help.", "🫶")
    )),
    Lesson(3, "KỂ CHUYỆN", "Kể chuyện đã xảy ra", "Bắt đầu dùng thì quá khứ đơn", "13 phút", "◷", Color(0xFFE8EEF9), listOf(
        Word("Yesterday", "/ˈjestərdeɪ/", "Hôm qua", "I visited my friend yesterday.", "📆"),
        Word("Last weekend", "/læst ˈwiːkend/", "Cuối tuần trước", "We went hiking last weekend.", "🥾"),
        Word("I went to...", "/aɪ went tuː/", "Tôi đã đi đến...", "I went to the beach.", "🏖️"),
        Word("It was fun", "/ɪt wəz fʌn/", "Chuyến đó vui lắm", "The concert was fun!", "🎉")
    )),
    Lesson(4, "SÂN BAY", "Làm thủ tục sân bay", "Tự tin hỏi thông tin và lên máy bay", "10 phút", "✈", Color(0xFFE6EFF4), listOf(
        Word("Boarding pass", "/ˈbɔːrdɪŋ pæs/", "Thẻ lên máy bay", "Here is my boarding pass.", "🎫"),
        Word("Check in", "/tʃek ɪn/", "Làm thủ tục", "Where can I check in?", "🧳"),
        Word("Window seat", "/ˈwɪndoʊ siːt/", "Ghế cạnh cửa sổ", "Could I have a window seat?", "🪟"),
        Word("What time does it board?", "/wʌt taɪm dʌz ɪt bɔːrd/", "Mấy giờ máy bay bắt đầu lên khách?", "What time does flight 205 board?", "⏰")
    )),
    Lesson(4, "KHÁCH SẠN", "Nhận phòng khách sạn", "Đặt phòng và xử lý yêu cầu thường gặp", "9 phút", "▣", Color(0xFFF9EEDC), listOf(
        Word("I have a reservation", "/aɪ hæv ə ˌrezərˈveɪʃən/", "Tôi đã đặt phòng", "I have a reservation under Nguyen.", "🛎️"),
        Word("Check-in time", "/tʃek ɪn taɪm/", "Giờ nhận phòng", "What is the check-in time?", "🕒"),
        Word("Could I get an extra towel?", "/kʊd aɪ ɡet ən ˈekstrə ˈtaʊəl/", "Cho tôi xin thêm khăn được không?", "Could I get an extra towel, please?", "🧺"),
        Word("The room key", "/ðə ruːm kiː/", "Chìa khóa phòng", "I left the room key inside.", "🔑")
    )),
    Lesson(4, "THỜI TIẾT", "Trò chuyện về thời tiết", "Bắt đầu cuộc trò chuyện tự nhiên", "8 phút", "☁", Color(0xFFE8EEF9), listOf(
        Word("It's sunny", "/ɪts ˈsʌni/", "Trời nắng", "It's sunny and warm today.", "☀️"),
        Word("It's pouring", "/ɪts ˈpɔːrɪŋ/", "Trời mưa như trút nước", "It's pouring outside.", "🌧️"),
        Word("A little chilly", "/ə ˈlɪtl ˈtʃɪli/", "Hơi se lạnh", "It's a little chilly this morning.", "🧥"),
        Word("What's the forecast?", "/wʌts ðə ˈfɔːrkæst/", "Dự báo thời tiết thế nào?", "What's the forecast for tomorrow?", "🌦️")
    )),
    Lesson(4, "SỨC KHỎE", "Mô tả triệu chứng", "Nói rõ vấn đề khi cần trợ giúp", "11 phút", "✚", Color(0xFFFCE9E8), listOf(
        Word("I have a headache", "/aɪ hæv ə ˈhedeɪk/", "Tôi bị đau đầu", "I have a headache and feel tired.", "🤕"),
        Word("I don't feel well", "/aɪ doʊnt fiːl wel/", "Tôi cảm thấy không khỏe", "I don't feel well today.", "🤒"),
        Word("Make an appointment", "/meɪk ən əˈpɔɪntmənt/", "Đặt lịch hẹn", "I'd like to make an appointment.", "📅"),
        Word("Where does it hurt?", "/wer dʌz ɪt hɜːrt/", "Bạn đau ở đâu?", "The nurse asked where it hurt.", "🩺")
    )),
    Lesson(4, "DI CHUYỂN", "Đi taxi trong thành phố", "Hướng dẫn tài xế và hỏi chi phí chuyến đi", "8 phút", "🚕", Color(0xFFF9EEDC), listOf(
        Word("Take me to this address", "/teɪk miː tuː ðɪs əˈdres/", "Đưa tôi đến địa chỉ này", "Could you take me to this address?", "📍"),
        Word("Stop here, please", "/stɑːp hɪr pliːz/", "Làm ơn dừng ở đây", "Stop here, please. I'll get out.", "🛑"),
        Word("How long will it take?", "/haʊ lɔːŋ wɪl ɪt teɪk/", "Sẽ mất bao lâu?", "How long will it take to get there?", "⏱️"),
        Word("Keep the change", "/kiːp ðə tʃeɪndʒ/", "Cứ giữ tiền thừa", "Here's the fare. Keep the change.", "💵")
    )),
    Lesson(5, "NHÀ CỬA", "Mô tả ngôi nhà", "Nói về căn phòng và nơi mình sống", "9 phút", "⌂", Color(0xFFE8F1EF), listOf(
        Word("Living room", "/ˈlɪvɪŋ ruːm/", "Phòng khách", "We watch movies in the living room.", "🛋️"),
        Word("On the second floor", "/ɑːn ðə ˈsekənd flɔːr/", "Ở tầng hai", "My bedroom is on the second floor.", "🏠"),
        Word("Next to", "/nekst tuː/", "Bên cạnh", "The kitchen is next to the dining room.", "↔️"),
        Word("It's quite spacious", "/ɪts kwaɪt ˈspeɪʃəs/", "Nó khá rộng rãi", "The apartment is quite spacious.", "📐")
    )),
    Lesson(5, "ĐIỆN THOẠI", "Gọi điện & nhắn tin", "Giao tiếp lịch sự qua điện thoại", "10 phút", "☎", Color(0xFFE6EFF4), listOf(
        Word("Can I call you back?", "/kæn aɪ kɔːl juː bæk/", "Tôi gọi lại cho bạn được không?", "I'm driving. Can I call you back?", "📞"),
        Word("Leave a message", "/liːv ə ˈmesɪdʒ/", "Để lại lời nhắn", "Could you leave a message?", "💬"),
        Word("The line is busy", "/ðə laɪn ɪz ˈbɪzi/", "Đường dây đang bận", "The line is busy right now.", "📵"),
        Word("I'll text you", "/aɪl tekst juː/", "Tôi sẽ nhắn tin cho bạn", "I'll text you the address.", "✉️")
    )),
    Lesson(5, "TÀI CHÍNH", "Ngân hàng & chi tiêu", "Hỏi về thanh toán, hóa đơn và ngân sách", "10 phút", "＄", Color(0xFFE8F1EF), listOf(
        Word("Open an account", "/ˈoʊpən ən əˈkaʊnt/", "Mở một tài khoản", "I'd like to open a bank account.", "🏦"),
        Word("Withdraw cash", "/wɪðˈdrɔː kæʃ/", "Rút tiền mặt", "Can I withdraw cash here?", "💵"),
        Word("What's the total?", "/wʌts ðə ˈtoʊtl/", "Tổng cộng là bao nhiêu?", "What's the total including tax?", "🧾"),
        Word("Stay within budget", "/steɪ wɪˈðɪn ˈbʌdʒɪt/", "Giữ trong phạm vi ngân sách", "I try to stay within my budget.", "📊")
    )),
    Lesson(5, "NGỮ PHÁP", "Nói về dự định tương lai", "Phân biệt kế hoạch và dự đoán", "12 phút", "➜", Color(0xFFFFEEE3), listOf(
        Word("I'm going to...", "/aɪm ˈɡoʊɪŋ tuː/", "Tôi dự định sẽ...", "I'm going to study tonight.", "🗓️"),
        Word("I will help", "/aɪ wɪl help/", "Tôi sẽ giúp", "Don't worry, I will help you.", "🤝"),
        Word("Are you free tomorrow?", "/ɑːr juː friː təˈmɑːroʊ/", "Ngày mai bạn có rảnh không?", "Are you free tomorrow evening?", "📆"),
        Word("Let's make a plan", "/lets meɪk ə plæn/", "Cùng lên kế hoạch nhé", "Let's make a plan for the trip.", "📝")
    )),
    Lesson(5, "PHỎNG VẤN", "Giới thiệu bản thân", "Chuẩn bị cho buổi phỏng vấn đầu tiên", "12 phút", "▤", Color(0xFFE8F1EF), listOf(
        Word("I have experience in...", "/aɪ hæv ɪkˈspɪriəns ɪn/", "Tôi có kinh nghiệm về...", "I have experience in customer service.", "💼"),
        Word("My strength is...", "/maɪ streŋθ ɪz/", "Thế mạnh của tôi là...", "My strength is solving problems.", "⭐"),
        Word("I'm interested in this role", "/aɪm ˈɪntrəstɪd ɪn ðɪs roʊl/", "Tôi quan tâm đến vị trí này", "I'm interested in this role.", "🎯"),
        Word("Thank you for your time", "/θæŋk juː fər jʊr taɪm/", "Cảm ơn anh/chị đã dành thời gian", "Thank you for your time today.", "🙏")
    ))
)