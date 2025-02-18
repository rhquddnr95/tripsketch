package kr.kro.tripsketch.services

import org.springframework.stereotype.Service
@Service
class NickNameService {
    private val countriesAndCities = listOf(
        "서울", "도쿄", "방콕", "도하", "리마", "로마", "밀라노", "피렌체", "쿠알라룸푸르", "시드니", "멜버른", "캔버라", "퍼스", "베를린", "뮌헨", "프랑크푸르트", "햄버그", "쾰른", "바르샤바", "크락프", "헬싱키", "오슬로", "리스본", "포르토", "마드리드", "바르셀로나", "발렌시아", "세비야", "그라나다", "말라가", "파리", "리옹", "마르세유", "니스", "스트라스부르", "보르도", "루블랑", "캉", "몬트리올", "퀘벡", "토론토", "오타와", "벤쿠버", "칼거리", "에드먼턴", "위니펙", "샤를로트타운", "세인트존", "프레데릭턴", "할리팩스", "뉴욕", "로스앤젤레스", "시카고", "휴스턴", "마이애미", "아틀란타", "보스턴", "시애틀", "샌프란시스코", "다라스", "디트로이트", "필라델피아", "피닉스", "샌디에이고", "샌안토니오", "샌호세", "잭슨빌", "인디애나폴리스", "콜럼버스", "샬럿", "포트워스", "엘패소", "멤피스", "나쉬빌", "포틀랜드", "오클라호마시티", "라스베이거스", "발티모어", "루이빌", "앨버쿼키", "투손", "프레즈노", "사크라멘토", "메사", "캔자스시티", "애틀랜타", "오마하", "오클랜드", "미니애폴리스", "탬파", "위치타", "뉴올리언스", "알링턴", "바커스필드", "아너하임", "헌츠빌", "애쉬빌", "렉싱턴", "아크론", "아마릴로", "오거스타", "머피스보로", "머세드", "버밍햄", "리치몬드", "디모인", "모빌", "스톡턴", "스포캔", "뉴어크", "프로보", "뉴헤이븐", "사바나", "플린트", "글렌데일", "리노", "루이스빌", "윈스턴세일럼", "헨더슨", "체서", "링컨", "페예트빌", "그린즈보로", "앵커리지", "에버렛", "오렌지", "바이로냐", "오클라호마", "칼리스펠", "루이스톤", "로펜", "바이저", "벨린엄", "코르데일레인", "펜스콜라", "스프링필드", "미사울라", "에버렛", "헤이든", "글렌데일", "피오리아", "스코츠토일", "예거", "슬리피아이", "샤를로트", "알람오소", "몬테레이", "애플턴", "벤턴빌", "아비린", "미들턴", "셰보이간", "모어헤드", "린치버그", "샐린", "헌츠빌", "칸자스", "페탈루마", "바레인", "일본", "캐나다", "호주", "브라질", "인도", "인도네시아", "브루나이", "싱가포르", "태국", "말레이시아", "필리핀", "베트남", "캄보디아", "라오스", "미얀마", "네팔", "부탄", "스리랑카", "몰디브"
    )
    private val adjectives = listOf(
        "아름다운", "매력적인", "유명한", "전통적인", "현대적인", "아늑한", "모험적인", "신비로운", "평화로운", "이색적인", "활기찬", "고요한", "행복한", "자유로운", "독특한", "유쾌한", "우아한", "열정적인", "용감한", "창의적인", "지적인", "친절한", "재미있는", "상냥한", "기발한", "예술적인", "호기심많은", "정열적인", "발랄한", "유머러스한", "침착한", "자신있는", "꾸밈없는", "세심한", "다정한", "스릴있는", "파란만장한", "열린 마음의", "현명한", "용맹한", "활발한", "신중한", "사려깊은", "성실한", "의지박약한", "끈기있는", "독립적인", "창조적인", "명랑한",
    )
    private val nouns = listOf(
        "여행자", "탐험가", "사진가", "음식가", "학자", "예술가", "운동가", "요리사", "뮤지션", "작가", "학생", "교사", "연구원", "디자이너", "엔지니어", "농부", "치과의사", "요가강사", "바리스타", "화가", "소설가", "건축가", "치어리더", "번역가", "연예인", "심리학자", "경찰관", "요리연구가", "간호사", "변호사", "회계사", "애니메이터", "프로그래머", "게임개발자", "운전사", "판사", "화학자", "물리학자", "천문학자", "지질학자", "도서관", "비행사", "심판", "마술사", "조종사", "광고인", "해커", "명상가", "등산가", "무용가", "연주자", "승무원", "사냥꾼", "요정", "도적", "캠퍼", "독서가", "컬렉터", "꽃집주인", "잠수부", "기자", "아티스트", "외계인", "히어로", "탐정", "농장주", "귀족", "프린스", "프린세스", "왕자", "왕녀", "용사", "마법사", "성기사", "궁수", "힐러", "초능력자", "요술사", "가수", "댄서", "개그맨", "코미디언", "공주", "공작", "시인", "백작", "마녀", "주술사", "무사", "검사", "술사", "대장", "영웅", "백성", "피아니스트", "드러머", "기타리스트", "베이시스트", "셰프", "파티셰", "소방관", "의사", "정치인", "군인", "모험가", "항공사", "건축사", "상인", "음악가", "사찰관", "철학자"
    )

    /**
     * `NickNameService` 클래스는 무작위로 닉네임을 생성하는 기능을 제공합니다.
     *
     * 이 서비스는 다음 카테고리의 요소를 조합하여 닉네임을 생성합니다:
     * - 여행 국가 및 도시 (countriesAndCities): 178개
     * - 형용사 (adjectives): 49개
     * - 명사 (nouns): 111개
     *
     * 최대 생성 가능한 닉네임의 조합은 789,403가지 입니다.
     * @author Hojun Song
     */

    fun generateRandomNickname(): String {
        val randomCountryOrCity = countriesAndCities.random()
        val randomAdjective = adjectives.random()
        val randomNoun = nouns.random()

        val nickname = "${randomCountryOrCity}의${randomAdjective}$randomNoun"
        return if (nickname.length <= 12) nickname else generateRandomNickname()
    }
}
