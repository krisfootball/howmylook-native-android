package com.howmylook.app.domain

data class LegalSection(
    val heading: String,
    val paragraphs: List<String>,
)

data class LegalDocument(
    val type: LegalDocumentType,
    val title: String,
    val lastUpdated: String,
    val intro: String? = null,
    val sections: List<LegalSection>,
)

object LegalDocuments {
    fun forType(type: LegalDocumentType): LegalDocument = when (type) {
        LegalDocumentType.Terms -> terms
        LegalDocumentType.Privacy -> privacy
        LegalDocumentType.Guidelines -> guidelines
        LegalDocumentType.Contact -> contact
    }

    val terms = LegalDocument(
        type = LegalDocumentType.Terms,
        title = "Terms of Service",
        lastUpdated = "May 7, 2026",
        intro = "These Terms of Service govern your access to and use of HowMyLook, a fashion feedback app where users post outfit photos, rate looks, and interact with other users.",
        sections = listOf(
            LegalSection(
                heading = "1. Acceptance of Terms",
                paragraphs = listOf(
                    "By creating an account, accessing, or using HowMyLook, you agree to these Terms. If you do not agree, do not use the app.",
                ),
            ),
            LegalSection(
                heading = "2. Eligibility",
                paragraphs = listOf(
                    "You must be at least 13 years old, or the minimum age required in your country to use online services like this. If you are under the age of legal majority where you live, you should use HowMyLook only with permission from a parent or legal guardian.",
                ),
            ),
            LegalSection(
                heading = "3. Your Account",
                paragraphs = listOf(
                    "You are responsible for the accuracy of the information you provide.",
                    "You are responsible for keeping your login credentials secure.",
                    "You are responsible for all activity that happens under your account.",
                    "You may not impersonate another person or create an account for deceptive or abusive purposes.",
                ),
            ),
            LegalSection(
                heading = "4. What HowMyLook Is",
                paragraphs = listOf(
                    "HowMyLook is a social fashion feedback product designed for quick yes/no outfit reactions and related discovery features. It is not a professional styling, medical, mental health, or safety service.",
                ),
            ),
            LegalSection(
                heading = "5. Content You Post",
                paragraphs = listOf(
                    "You keep ownership of the content you submit, but you give HowMyLook a worldwide, non-exclusive, royalty-free license to host, store, reproduce, modify for technical display, and show your content for the purpose of operating, improving, and promoting the app.",
                    "You represent that you have the rights needed to post the content you upload, including any photos, likenesses, and other material.",
                ),
            ),
            LegalSection(
                heading = "6. Prohibited Content and Conduct",
                paragraphs = listOf(
                    "You may not post, upload, share, or otherwise use HowMyLook to distribute:",
                    "Nudity, sexually explicit content, or exploitative content",
                    "Content involving minors in inappropriate, sexualized, or unsafe contexts",
                    "Harassment, threats, hateful conduct, or bullying",
                    "Graphic violence or shocking content",
                    "Spam, scams, or deceptive promotions",
                    "Content that infringes another person’s privacy, publicity, or intellectual property rights",
                    "Non-outfit content that clearly does not fit the purpose of the app",
                    "Anything unlawful or intended to harm people, systems, or the service",
                ),
            ),
            LegalSection(
                heading = "7. Moderation and Enforcement",
                paragraphs = listOf(
                    "We may review, hide, remove, restrict, or delete content or accounts at our discretion when content violates these Terms, our guidelines, applicable law, or the intended use of the app. We may do this with or without prior notice.",
                ),
            ),
            LegalSection(
                heading = "8. Feedback and Ratings",
                paragraphs = listOf(
                    "Ratings and opinions from other users are subjective. We do not guarantee the accuracy, fairness, usefulness, or tone of user feedback.",
                ),
            ),
            LegalSection(
                heading = "9. Privacy",
                paragraphs = listOf(
                    "Your use of HowMyLook is also governed by our Privacy Policy, which explains how we collect, use, and store information.",
                ),
            ),
            LegalSection(
                heading = "10. Termination",
                paragraphs = listOf(
                    "You may stop using the app at any time. We may suspend or terminate access to the app, or remove content, at any time if we believe you have violated these Terms, created risk for the service, or used the app in an abusive or unlawful way.",
                ),
            ),
            LegalSection(
                heading = "11. Disclaimer",
                paragraphs = listOf(
                    "HowMyLook is provided on an “as is” and “as available” basis without warranties of any kind, to the maximum extent allowed by law.",
                ),
            ),
            LegalSection(
                heading = "12. Limitation of Liability",
                paragraphs = listOf(
                    "To the maximum extent permitted by law, HowMyLook and its operators will not be liable for indirect, incidental, special, consequential, or punitive damages, or for any loss of data, reputation, profits, or business arising from your use of the app.",
                ),
            ),
            LegalSection(
                heading = "13. Changes to These Terms",
                paragraphs = listOf(
                    "We may update these Terms from time to time. Continued use of the app after an update means you accept the revised Terms.",
                ),
            ),
            LegalSection(
                heading = "14. Contact",
                paragraphs = listOf(
                    "If you need to contact the operator of HowMyLook about these Terms, use the Contact & Support page in this app.",
                ),
            ),
        ),
    )

    val privacy = LegalDocument(
        type = LegalDocumentType.Privacy,
        title = "Privacy Policy",
        lastUpdated = "May 7, 2026",
        intro = "This Privacy Policy explains how HowMyLook collects, uses, stores, and shares information when you use the app.",
        sections = listOf(
            LegalSection(
                heading = "1. Information We Collect",
                paragraphs = listOf(
                    "Account information such as your email address and login credentials",
                    "Profile information such as username, display name, avatar, and bio if you provide them",
                    "Content you upload, including outfit photos and occasion text",
                    "Activity data such as ratings, follows, notifications, and moderation actions related to your account",
                    "Technical data such as device, browser, app interaction, and log information needed to operate and secure the app",
                ),
            ),
            LegalSection(
                heading = "2. How We Use Information",
                paragraphs = listOf(
                    "To create and manage your account",
                    "To display your profile and content within the app",
                    "To power ratings, discovery, social features, moderation, and notifications",
                    "To operate, maintain, secure, and improve the service",
                    "To investigate abuse, enforce policies, and comply with legal obligations",
                ),
            ),
            LegalSection(
                heading = "3. How Your Content Is Visible",
                paragraphs = listOf(
                    "Content you post may be shown to other users of the app, including your outfit photos, display name, username, profile information, follower relationships, and visible taste signals such as yes/no activity where the product makes that public.",
                ),
            ),
            LegalSection(
                heading = "4. Moderation and Safety",
                paragraphs = listOf(
                    "We may review account and content data to detect abuse, enforce our rules, and keep the service aligned with its intended purpose.",
                ),
            ),
            LegalSection(
                heading = "5. Storage and Service Providers",
                paragraphs = listOf(
                    "HowMyLook uses third-party infrastructure and service providers to host the app, store data, authenticate users, and support related product functions. Your information may be processed by those providers on our behalf.",
                ),
            ),
            LegalSection(
                heading = "6. Data Retention",
                paragraphs = listOf(
                    "We keep information for as long as needed to operate the app, enforce our terms, resolve disputes, comply with legal obligations, and improve the product. Some posts may expire under product rules, while moderation and operational records may be kept longer.",
                ),
            ),
            LegalSection(
                heading = "7. Your Choices",
                paragraphs = listOf(
                    "You can choose what profile details and content to provide.",
                    "You can stop using the app at any time.",
                    "You may request deletion or support help through our contact and support process.",
                ),
            ),
            LegalSection(
                heading = "8. Account Deletion and Data Requests",
                paragraphs = listOf(
                    "You may request account deletion, access to your data, or another privacy-related request by contacting HowMyLook support. For safety, we may need to verify that the request came from the account owner before taking action.",
                    "We may retain certain information where necessary for legal compliance, fraud prevention, dispute resolution, enforcement, or legitimate operational record-keeping.",
                    "You can also request account deletion from Edit profile in this app. We email you a confirmation link, and your account is only deleted after you open that link.",
                ),
            ),
            LegalSection(
                heading = "9. Children",
                paragraphs = listOf(
                    "HowMyLook is not intended for children below the minimum age required to use the service where they live. If you believe a child has provided personal information in violation of this policy, contact the operator so the issue can be reviewed.",
                ),
            ),
            LegalSection(
                heading = "10. International Use",
                paragraphs = listOf(
                    "Your information may be processed in countries other than the one where you live, depending on hosting and service providers.",
                ),
            ),
            LegalSection(
                heading = "11. Changes to This Policy",
                paragraphs = listOf(
                    "We may update this Privacy Policy from time to time. Continued use of the app after changes means the updated policy applies.",
                ),
            ),
            LegalSection(
                heading = "12. Contact",
                paragraphs = listOf(
                    "Visit Contact & Support in this app for support, privacy questions, and account deletion requests.",
                ),
            ),
        ),
    )

    val guidelines = LegalDocument(
        type = LegalDocumentType.Guidelines,
        title = "Community Guidelines",
        lastUpdated = "May 7, 2026",
        intro = "HowMyLook is for real outfit feedback. These guidelines explain what belongs in the app and what may be removed.",
        sections = listOf(
            LegalSection(
                heading = "What belongs here",
                paragraphs = listOf(
                    "Real outfit photos",
                    "Looks you want feedback on",
                    "Clear, honest occasion text",
                    "Respectful participation and rating behavior",
                ),
            ),
            LegalSection(
                heading = "What does not belong here",
                paragraphs = listOf(
                    "Nudity or sexually explicit content",
                    "Content involving minors in unsafe or inappropriate contexts",
                    "Harassment, bullying, hate, threats, or humiliation",
                    "Spam, scams, fake engagement, or misleading content",
                    "Graphic violence or shocking material",
                    "Copyright-infringing or privacy-violating content",
                    "Images that are not really about outfit feedback",
                ),
            ),
            LegalSection(
                heading = "Moderation",
                paragraphs = listOf(
                    "Posts that do not fit the app’s purpose or violate these rules may be removed. Accounts that repeatedly abuse the app may be restricted or suspended.",
                ),
            ),
            LegalSection(
                heading = "How to use the app well",
                paragraphs = listOf(
                    "Post clear photos of the outfit you want feedback on",
                    "Keep ratings honest and quick",
                    "Be respectful of other people’s appearance and participation",
                ),
            ),
        ),
    )

    val contact = LegalDocument(
        type = LegalDocumentType.Contact,
        title = "Contact & Support",
        lastUpdated = "May 7, 2026",
        intro = "If you need help with your account, privacy questions, moderation issues, or a data request, contact HowMyLook support.",
        sections = listOf(
            LegalSection(
                heading = "Support contact",
                paragraphs = listOf(
                    "Email: support@howmylook.com",
                    "Use this same inbox for support, privacy requests, and account deletion requests.",
                ),
            ),
            LegalSection(
                heading = "What you can contact us about",
                paragraphs = listOf(
                    "Account access issues",
                    "Post moderation questions",
                    "Report abuse or harmful content",
                    "Request account deletion",
                    "Request help with your personal data",
                ),
            ),
            LegalSection(
                heading = "Account deletion and data requests",
                paragraphs = listOf(
                    "If you want your account deleted or want to make a privacy-related request, contact support from the email address linked to your account and include enough detail for us to verify the request and help you safely.",
                    "You can also request account deletion from Edit profile in this app. We email you a confirmation link, and your account is only deleted after you open that link.",
                ),
            ),
        ),
    )
}
