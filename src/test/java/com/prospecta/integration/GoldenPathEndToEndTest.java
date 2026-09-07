package com.prospecta.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.ai.domain.AiPromptTemplate;
import com.prospecta.ai.dto.AiDto.AiRequest;
import com.prospecta.ai.dto.AiDto.AiResponse;
import com.prospecta.ai.provider.AiProvider;
import com.prospecta.ai.repository.AiPromptTemplateRepository;
import com.prospecta.ai.service.QuotaService;
import com.prospecta.analytics.dto.AnalyticsDto.AnalyticsOverviewResponse;
import com.prospecta.analytics.service.AnalyticsService;
import com.prospecta.campaign.domain.*;
import com.prospecta.campaign.repository.CampaignProspectRepository;
import com.prospecta.campaign.repository.CampaignRepository;
import com.prospecta.campaign.repository.CampaignStepRepository;
import com.prospecta.campaign.service.CampaignExecutionService;
import com.prospecta.campaign.service.CampaignService;
import com.prospecta.conversation.domain.Conversation;
import com.prospecta.conversation.domain.ConversationMessage;
import com.prospecta.conversation.domain.ConversationStatus;
import com.prospecta.conversation.dto.ConversationDto.AiReplySuggestion;
import com.prospecta.conversation.dto.ConversationDto.ConversationMessageDto;
import com.prospecta.conversation.repository.ConversationMessageRepository;
import com.prospecta.conversation.repository.ConversationRepository;
import com.prospecta.conversation.service.ConversationService;
import com.prospecta.identity.domain.UserProfile;
import com.prospecta.identity.domain.UserRole;
import com.prospecta.identity.domain.UserStatus;
import com.prospecta.identity.repository.UserProfileRepository;
import com.prospecta.messaging.domain.Message;
import com.prospecta.messaging.domain.MessageDirection;
import com.prospecta.messaging.domain.MessageStatus;
import com.prospecta.messaging.repository.ExternalEventRepository;
import com.prospecta.messaging.repository.MessageRepository;
import com.prospecta.messaging.service.MessagingService;
import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.organization.repository.OrganizationRepository;
import com.prospecta.pipeline.domain.Opportunity;
import com.prospecta.pipeline.domain.OpportunityStage;
import com.prospecta.pipeline.dto.OpportunityDto.CreateOpportunityRequest;
import com.prospecta.pipeline.dto.OpportunityDto.OpportunityResponse;
import com.prospecta.pipeline.dto.OpportunityDto.UpdateOpportunityStageRequest;
import com.prospecta.pipeline.repository.OpportunityRepository;
import com.prospecta.pipeline.service.PipelineService;
import com.prospecta.prospect.domain.*;
import com.prospecta.prospect.dto.LeadScoreResult;
import com.prospecta.prospect.repository.CompanyRepository;
import com.prospecta.prospect.repository.IcpRepository;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.prospect.service.LeadScoringEngine;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.outbox.OutboxService;
import com.prospecta.shared.security.TenantContext;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.security.UserPrincipal;
import com.prospecta.shared.utils.PhoneNumberUtils;
import com.prospecta.shared.utils.SsrfValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * End-to-End Golden Path Demonstration Test
 *
 * Demonstrates the 21-step commercial workflow of Prospecta SaaS
 * tailored for the Senegalese & West African market (+221, XOF, Africa/Dakar):
 *
 * 1. Organization creation (SaaS tenant setup)
 * 2. User profile creation (Sales Org Admin)
 * 3. Ideal Customer Profile (ICP) definition
 * 4. Prospect import & phone normalization (+221)
 * 5. SSRF validation & security check
 * 6. Company creation & prospect linking
 * 7. AI Lead Scoring Engine Execution (+20 SN, +25 phone, +15 email, +30 title)
 * 8. Prospect status transition to QUALIFIED
 * 9. Multichannel campaign creation
 * 10. Sequence configuration (Email position 1, WhatsApp position 2)
 * 11. Target prospect linkage
 * 12. Campaign launch (status RUNNING, target ACTIVE)
 * 13. Campaign position 1 execution (personalized email outbox event)
 * 14. Step advancement to nextActionAt (+2 days)
 * 15. Inbound WhatsApp webhook message received
 * 16. Inbound message recorded in Unified Inbox & Prospect auto-transitions to REPLIED
 * 17. Sequence engine automatically halts further outreach for replied prospect
 * 18. Unified Inbox conversation consultation
 * 19. AI Reply Copilot (CONVERSATION_REPLY_V1) generates intelligent response suggestion
 * 20. Sales deal / Opportunity created in CRM Pipeline (3,500,000 XOF)
 * 21. Opportunity won (100% win rate) & Analytics overview verified in XOF
 */
@ExtendWith(MockitoExtension.class)
class GoldenPathEndToEndTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private IcpRepository icpRepository;
    @Mock private ProspectRepository prospectRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignStepRepository campaignStepRepository;
    @Mock private CampaignProspectRepository campaignProspectRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ExternalEventRepository externalEventRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationMessageRepository conversationMessageRepository;
    @Mock private OpportunityRepository opportunityRepository;
    @Mock private AiPromptTemplateRepository promptTemplateRepository;
    @Mock private AiProvider aiProvider;
    @Mock private QuotaService quotaService;
    @Mock private OutboxService outboxService;
    @Mock private AuditService auditService;
    @Mock private MessagingService messagingService;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    private LeadScoringEngine scoringEngine;
    private CampaignService campaignService;
    private CampaignExecutionService campaignExecutionService;
    private ConversationService conversationService;
    private PipelineService pipelineService;
    private AnalyticsService analyticsService;

    private UUID organizationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        TenantContextHolder.setContext(TenantContext.builder()
                .organizationId(organizationId)
                .userPrincipal(UserPrincipal.builder()
                        .userId(userId)
                        .email("aminata.fall@terangadigital.sn")
                        .organizationId(organizationId)
                        .role(UserRole.ORG_ADMIN)
                        .permissions(Set.of("CAMPAIGNS_MANAGE", "PROSPECTS_MANAGE", "PIPELINE_MANAGE"))
                        .build())
                .build());

        scoringEngine = new LeadScoringEngine(icpRepository);

        campaignService = new CampaignService(
                campaignRepository, campaignStepRepository, campaignProspectRepository,
                prospectRepository, outboxService, auditService
        );

        campaignExecutionService = new CampaignExecutionService(
                campaignProspectRepository, campaignStepRepository, prospectRepository, outboxService
        );

        conversationService = new ConversationService(
                conversationRepository, conversationMessageRepository, prospectRepository,
                messagingService, promptTemplateRepository, aiProvider, quotaService, auditService, objectMapper
        );

        pipelineService = new PipelineService(
                opportunityRepository, prospectRepository, companyRepository, auditService
        );

        analyticsService = new AnalyticsService(
                prospectRepository, campaignRepository, campaignProspectRepository, messageRepository, opportunityRepository
        );
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Golden Path E2E: Complete 21-step commercial sequence from Org setup to Won Opportunity")
    void testCompleteGoldenPathWorkflow() {
        // =========================================================================
        // STEP 1: Organization Creation (Tenant Provisioning)
        // =========================================================================
        Organization org = Organization.builder()
                .name("Teranga Digital Solutions")
                .slug("teranga-digital-solutions")
                .country("SN")
                .currency("XOF")
                .timezone("Africa/Dakar")
                .plan(OrganizationPlan.STARTER)
                .build();
        org.setId(organizationId);

        assertThat(org.getCountry()).isEqualTo("SN");
        assertThat(org.getCurrency()).isEqualTo("XOF");
        assertThat(org.getTimezone()).isEqualTo("Africa/Dakar");

        // =========================================================================
        // STEP 2: User Profile Setup (Sales Org Admin)
        // =========================================================================
        UserProfile adminUser = UserProfile.builder()
                .organization(org)
                .keycloakSubject("kc-user-001")
                .email("aminata.fall@terangadigital.sn")
                .firstName("Aminata")
                .lastName("Fall")
                .role(UserRole.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        adminUser.setId(userId);

        assertThat(adminUser.getRole()).isEqualTo(UserRole.ORG_ADMIN);
        assertThat(adminUser.getOrganizationId()).isEqualTo(organizationId);

        // =========================================================================
        // STEP 3: Define Ideal Customer Profile (ICP for Senegal)
        // =========================================================================
        IdealCustomerProfile icp = IdealCustomerProfile.builder()
                .name("ICP Hôtellerie & PME Dakar")
                .targetCountries("SN")
                .targetIndustries("Hôtellerie, Tourisme, E-commerce")
                .targetCities("Dakar")
                .minEmployees(10)
                .maxEmployees(200)
                .build();
        icp.setId(UUID.randomUUID());
        icp.setOrganizationId(organizationId);

        // =========================================================================
        // STEP 4: Import Prospect (Raw contact with Senegalese phone)
        // =========================================================================
        String rawPhone = "+221 77 123 45 67";
        Optional<String> normalizedPhoneOpt = PhoneNumberUtils.normalizeToE164(rawPhone, "SN");
        assertThat(normalizedPhoneOpt).isPresent();
        String normalizedPhone = normalizedPhoneOpt.get();
        assertThat(normalizedPhone).isEqualTo("+221771234567");
        assertThat(PhoneNumberUtils.isValidNumber(normalizedPhone)).isTrue();

        // =========================================================================
        // STEP 5: SSRF Security Validation on Company URL
        // =========================================================================
        String companyUrl = "https://example.com";
        assertThat(SsrfValidator.isSafeUrl("http://169.254.169.254/latest/meta-data")).isFalse();
        assertThat(SsrfValidator.isSafeUrl("http://localhost:8080/admin")).isFalse();
        assertThat(SsrfValidator.isSafeUrl("file:///etc/passwd")).isFalse();
        assertThat(SsrfValidator.isSafeUrl("http://192.168.1.1/internal")).isFalse();

        // =========================================================================
        // STEP 6: Company Creation & Prospect Linking
        // =========================================================================
        UUID companyId = UUID.randomUUID();
        Company company = Company.builder()
                .name("Hôtel Teranga Dakar")
                .website(companyUrl)
                .industry("Hôtellerie")
                .employeeCount(65)
                .country("SN")
                .city("Dakar")
                .build();
        company.setId(companyId);
        company.setOrganizationId(organizationId);

        UUID prospectId = UUID.randomUUID();
        Prospect prospect = Prospect.builder()
                .company(company)
                .companyName(company.getName())
                .firstName("Fatou")
                .lastName("Sow")
                .fullName("Fatou Sow")
                .jobTitle("Directrice Générale")
                .email("fatou.sow@hoteldakar.sn")
                .phone(normalizedPhone)
                .whatsappNumber(normalizedPhone)
                .country("SN")
                .city("Dakar")
                .status(ProspectStatus.NEW)
                .build();
        prospect.setId(prospectId);
        prospect.setOrganizationId(organizationId);

        // =========================================================================
        // STEP 7 & 8: AI Lead Scoring Engine Execution (+20 SN, +25 phone, +15 email, +30 title)
        // =========================================================================
        LeadScoreResult scoreResult = scoringEngine.score(prospect, icp);
        assertThat(scoreResult.getScore()).isGreaterThanOrEqualTo(70);
        assertThat(scoreResult.getLevel()).isIn(LeadScoreLevel.HIGH, LeadScoreLevel.VERY_HIGH);
        assertThat(scoreResult.getReasons()).anyMatch(s -> s.contains("Sénégal"));
        assertThat(scoreResult.getReasons()).anyMatch(s -> s.contains("décisionnaire"));

        // Transition prospect to QUALIFIED
        prospect.setStatus(ProspectStatus.QUALIFIED);
        prospect.setLeadScore(scoreResult.getScore());
        prospect.setLeadScoreLevel(scoreResult.getLevel());
        assertThat(prospect.getStatus()).isEqualTo(ProspectStatus.QUALIFIED);

        // =========================================================================
        // STEP 9 & 10: Multichannel Campaign & Sequences Setup (Email -> WhatsApp)
        // =========================================================================
        UUID campaignId = UUID.randomUUID();
        Campaign campaign = Campaign.builder()
                .name("Campagne Hôtels Dakar 2026")
                .description("Offre Sales Automation pour hôtels du Sénégal")
                .channelStrategy("MULTI_CHANNEL")
                .status(CampaignStatus.DRAFT)
                .build();
        campaign.setId(campaignId);
        campaign.setOrganizationId(organizationId);

        CampaignStep step1 = CampaignStep.builder()
                .campaign(campaign)
                .position(1)
                .channel(ChannelType.EMAIL)
                .subjectTemplate("Optimisation de vos réservations directes - {{companyName}}")
                .contentTemplate("Bonjour {{firstName}}, nous aidons les hôtels à Dakar à automatiser leurs réservations.")
                .delayMinutes(0)
                .enabled(true)
                .build();
        step1.setId(UUID.randomUUID());

        CampaignStep step2 = CampaignStep.builder()
                .campaign(campaign)
                .position(2)
                .channel(ChannelType.WHATSAPP)
                .contentTemplate("Bonjour {{firstName}}, avez-vous pu jeter un coup d'œil à notre email concernant {{companyName}} ?")
                .delayMinutes(2880) // 48h delay
                .enabled(true)
                .build();
        step2.setId(UUID.randomUUID());

        campaign.setSteps(List.of(step1, step2));

        // =========================================================================
        // STEP 11 & 12: Target Prospect Linkage & Campaign Launch
        // =========================================================================
        CampaignProspect target = CampaignProspect.builder()
                .campaign(campaign)
                .prospect(prospect)
                .currentStep(1)
                .status(CampaignProspectStatus.ACTIVE)
                .nextActionAt(Instant.now().minusSeconds(10)) // due for execution
                .build();
        target.setId(UUID.randomUUID());

        campaign.setStatus(CampaignStatus.RUNNING);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RUNNING);

        // =========================================================================
        // STEP 13 & 14: Step 1 Execution (Personalized Email & Scheduling Step 2)
        // =========================================================================
        when(campaignProspectRepository.findDueProspects(eq(CampaignProspectStatus.ACTIVE), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(target));
        when(campaignStepRepository.findByCampaignIdAndPosition(campaignId, 1))
                .thenReturn(Optional.of(step1));
        when(campaignStepRepository.findByCampaignIdAndPosition(campaignId, 2))
                .thenReturn(Optional.of(step2));

        int processed = campaignExecutionService.executeDueSteps();
        assertThat(processed).isEqualTo(1);

        // Target must advance to Step 2 with delay (2880 min = 48h)
        assertThat(target.getCurrentStep()).isEqualTo(2);
        assertThat(target.getNextActionAt()).isAfter(Instant.now().plusSeconds(2800 * 60));
        // Prospect status transitioned to CONTACTED
        assertThat(prospect.getStatus()).isEqualTo(ProspectStatus.CONTACTED);

        verify(outboxService).recordEvent(eq("Message"), eq(prospect.getId().toString()), eq("message.send.requested"), any());

        // =========================================================================
        // STEP 15 & 16: Inbound WhatsApp Webhook (Prospect replies via WhatsApp)
        // =========================================================================
        String inboundText = "Bonjour Aminata, merci pour votre message. Nous souhaitons automatiser nos réservations. Pouvons-nous échanger jeudi à 11h ?";
        String externalWamid = "wamid.GOLDEN_INBOUND_001";

        Conversation conversation = Conversation.builder()
                .prospect(prospect)
                .channel(ChannelType.WHATSAPP)
                .status(ConversationStatus.OPEN)
                .build();
        conversation.setId(UUID.randomUUID());
        conversation.setOrganizationId(organizationId);

        when(prospectRepository.findByIdAndOrganizationId(prospectId, organizationId))
                .thenReturn(Optional.of(prospect));
        when(conversationRepository.findByOrganizationIdAndProspectIdAndChannel(organizationId, prospectId, ChannelType.WHATSAPP))
                .thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));
        when(conversationMessageRepository.save(any(ConversationMessage.class))).thenAnswer(i -> {
            ConversationMessage m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        ConversationMessageDto recordedMsg = conversationService.recordInboundMessage(
                organizationId, prospectId, ChannelType.WHATSAPP,
                normalizedPhone, "Prospecta Bot", inboundText, externalWamid
        );

        assertThat(recordedMsg).isNotNull();
        assertThat(recordedMsg.getDirection()).isEqualTo(MessageDirection.INBOUND);
        assertThat(recordedMsg.getContent()).contains("jeudi à 11h");

        // CRITICAL: Prospect status automatically transitioned to REPLIED
        assertThat(prospect.getStatus()).isEqualTo(ProspectStatus.REPLIED);
        verify(prospectRepository, atLeastOnce()).save(prospect);

        // =========================================================================
        // STEP 17: Sequence Engine Halts Further Outreach for Replied Prospect
        // =========================================================================
        // When campaign execution service runs, if prospect is REPLIED, it stops sequence
        target.setStatus(CampaignProspectStatus.ACTIVE);
        when(campaignProspectRepository.findDueProspects(eq(CampaignProspectStatus.ACTIVE), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(target));

        campaignExecutionService.executeDueSteps();
        assertThat(target.getStatus()).isEqualTo(CampaignProspectStatus.REPLIED);

        // =========================================================================
        // STEP 18 & 19: Unified Inbox Consultation & AI Reply Copilot (CONVERSATION_REPLY_V1)
        // =========================================================================
        when(conversationRepository.findByIdAndOrganizationId(conversation.getId(), organizationId))
                .thenReturn(Optional.of(conversation));

        ConversationMessage inboundEntity = ConversationMessage.builder()
                .conversation(conversation)
                .direction(MessageDirection.INBOUND)
                .channel(ChannelType.WHATSAPP)
                .content(inboundText)
                .sentAt(Instant.now())
                .build();
        when(conversationMessageRepository.findAllByConversationIdOrderBySentAtAsc(conversation.getId()))
                .thenReturn(List.of(inboundEntity));

        when(promptTemplateRepository.findByNameAndActiveTrue("CONVERSATION_REPLY_V1"))
                .thenReturn(Optional.of(AiPromptTemplate.builder()
                        .systemPrompt("Tu es un copilote commercial pour le marché sénégalais.")
                        .userPromptTemplate("Historique: {conversationHistory}")
                        .build()));

        String copilotJson = """
                {
                  "suggestedReply": "Bonjour Madame Sow, c'est noté avec grand plaisir ! Je bloque ce jeudi à 11h pour notre démonstration. Excellente journée !",
                  "intent": "DEMO_REQUEST",
                  "sentiment": "HIGHLY_POSITIVE",
                  "recommendedNextAction": "CREATE_OPPORTUNITY_AND_INVITE",
                  "confidence": 0.98
                }
                """;

        when(aiProvider.getProviderName()).thenReturn("openai");
        when(aiProvider.generate(any(AiRequest.class))).thenReturn(AiResponse.builder()
                .content(copilotJson)
                .model("gpt-4o-mini")
                .inputTokens(180)
                .outputTokens(85)
                .durationMs(510)
                .build());

        AiReplySuggestion aiSuggestion = conversationService.generateAiReply(conversation.getId());
        assertThat(aiSuggestion.getIntent()).isEqualTo("DEMO_REQUEST");
        assertThat(aiSuggestion.getSentiment()).isEqualTo("HIGHLY_POSITIVE");
        assertThat(aiSuggestion.getSuggestedReply()).contains("jeudi à 11h");
        assertThat(aiSuggestion.getConfidence()).isEqualTo(0.98);

        // =========================================================================
        // STEP 20: Sales Deal / Opportunity Created in CRM Pipeline
        // =========================================================================
        UUID opportunityId = UUID.randomUUID();
        CreateOpportunityRequest oppRequest = CreateOpportunityRequest.builder()
                .prospectId(prospectId)
                .companyId(companyId)
                .assignedTo(userId)
                .title("Hôtel Teranga - Licence Annuelle Prospecta")
                .stage(OpportunityStage.MEETING_SCHEDULED)
                .estimatedValue(BigDecimal.valueOf(3_500_000))
                .currency("XOF")
                .winProbability(60)
                .expectedCloseDate(LocalDate.now().plusWeeks(2))
                .notes("Rendez-vous démo calé ce jeudi 11h via WhatsApp")
                .build();

        when(prospectRepository.findByIdAndOrganizationId(prospectId, organizationId))
                .thenReturn(Optional.of(prospect));
        when(companyRepository.findByIdAndOrganizationId(companyId, organizationId))
                .thenReturn(Optional.of(company));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(i -> {
            Opportunity o = i.getArgument(0);
            o.setId(opportunityId);
            return o;
        });

        OpportunityResponse oppResponse = pipelineService.createOpportunity(oppRequest);
        assertThat(oppResponse).isNotNull();
        assertThat(oppResponse.getTitle()).isEqualTo("Hôtel Teranga - Licence Annuelle Prospecta");
        assertThat(oppResponse.getEstimatedValue()).isEqualByComparingTo(BigDecimal.valueOf(3_500_000));
        assertThat(oppResponse.getCurrency()).isEqualTo("XOF");
        assertThat(oppResponse.getStage()).isEqualTo(OpportunityStage.MEETING_SCHEDULED);
        assertThat(prospect.getStatus()).isEqualTo(ProspectStatus.OPPORTUNITY);

        // =========================================================================
        // STEP 21: Advance Opportunity to WON & Verify Analytics
        // =========================================================================
        Opportunity createdOpp = Opportunity.builder()
                .prospect(prospect)
                .company(company)
                .title(oppResponse.getTitle())
                .stage(OpportunityStage.MEETING_SCHEDULED)
                .estimatedValue(BigDecimal.valueOf(3_500_000))
                .currency("XOF")
                .winProbability(60)
                .build();
        createdOpp.setId(opportunityId);
        createdOpp.setOrganizationId(organizationId);

        when(opportunityRepository.findByIdAndOrganizationId(opportunityId, organizationId))
                .thenReturn(Optional.of(createdOpp));

        UpdateOpportunityStageRequest stageReq = UpdateOpportunityStageRequest.builder()
                .stage(OpportunityStage.WON)
                .build();

        OpportunityResponse wonResponse = pipelineService.updateStage(opportunityId, stageReq);
        assertThat(wonResponse.getStage()).isEqualTo(OpportunityStage.WON);
        assertThat(wonResponse.getWinProbability()).isEqualTo(100);
        assertThat(wonResponse.getClosedAt()).isNotNull();
        // Prospect status updated to WON
        assertThat(prospect.getStatus()).isEqualTo(ProspectStatus.WON);

        // Verify Analytics Overview
        when(prospectRepository.countByOrganizationId(organizationId)).thenReturn(1L);
        when(prospectRepository.countByOrganizationIdAndStatus(organizationId, ProspectStatus.QUALIFIED)).thenReturn(0L);
        when(prospectRepository.countByOrganizationIdAndStatus(organizationId, ProspectStatus.CONTACTED)).thenReturn(0L);
        when(prospectRepository.countByOrganizationIdAndStatus(organizationId, ProspectStatus.REPLIED)).thenReturn(1L);
        when(prospectRepository.countByOrganizationIdAndStatus(organizationId, ProspectStatus.MEETING_BOOKED)).thenReturn(0L);

        when(opportunityRepository.countByOrganizationId(organizationId)).thenReturn(1L);
        when(opportunityRepository.countByOrganizationIdAndStage(organizationId, OpportunityStage.WON)).thenReturn(1L);
        when(opportunityRepository.sumEstimatedValueByStage(organizationId, OpportunityStage.WON)).thenReturn(BigDecimal.valueOf(3_500_000));
        when(opportunityRepository.sumTotalEstimatedValue(organizationId)).thenReturn(BigDecimal.valueOf(3_500_000));

        when(campaignRepository.countByOrganizationIdAndStatus(organizationId, CampaignStatus.RUNNING)).thenReturn(1L);

        when(messageRepository.countByOrganizationIdAndChannel(eq(organizationId), any(ChannelType.class))).thenReturn(1L);
        when(messageRepository.countByOrganizationIdAndChannelAndStatus(eq(organizationId), any(ChannelType.class), any(MessageStatus.class))).thenReturn(1L);

        AnalyticsOverviewResponse analytics = analyticsService.getOverview();
        assertThat(analytics.getTotalProspects()).isEqualTo(1L);
        assertThat(analytics.getOpportunitiesWon()).isEqualTo(1L);
        assertThat(analytics.getTotalWonValue()).isEqualByComparingTo(BigDecimal.valueOf(3_500_000));
        assertThat(analytics.getCurrency()).isEqualTo("XOF");
        assertThat(analytics.getConversionRate()).isEqualTo(100.0);
        assertThat(analytics.getReplyRate()).isEqualTo(100.0);
    }
}
