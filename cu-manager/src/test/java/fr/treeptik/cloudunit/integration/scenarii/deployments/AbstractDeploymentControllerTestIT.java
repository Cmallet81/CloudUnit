package fr.treeptik.cloudunit.integration.scenarii.deployments;

import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.initializer.CloudUnitApplicationContext;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.UserService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.inject.Inject;
import javax.servlet.Filter;
import java.util.Random;

import static fr.treeptik.cloudunit.utils.TestUtils.downloadAndPrepareFileToDeploy;
import static fr.treeptik.cloudunit.utils.TestUtils.getUrlContentPage;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith( SpringJUnit4ClassRunner.class )
@WebAppConfiguration
@ContextConfiguration( classes = { CloudUnitApplicationContext.class, MockServletContext.class } )
@FixMethodOrder( MethodSorters.NAME_ASCENDING )
@ActiveProfiles( "integration" )
public abstract class AbstractDeploymentControllerTestIT

{

    private static String applicationName;

    private final Logger logger = LoggerFactory.getLogger( AbstractDeploymentControllerTestIT.class );

    protected String release;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Inject
    private AuthenticationManager authenticationManager;

    @Autowired
    private Filter springSecurityFilterChain;

    @Inject
    private UserService userService;

    private MockHttpSession session;

    @BeforeClass
    public static void initEnv()
    {
        applicationName = "App" + new Random().nextInt(100000);
    }

    @Before
    public void setup()
    {
        logger.info( "setup" );

        this.mockMvc = MockMvcBuilders.webAppContextSetup( context ).addFilters( springSecurityFilterChain ).build();

        User user = null;
        try
        {
            user = userService.findByLogin( "johndoe" );
        }
        catch ( ServiceException e )
        {
            logger.error( e.getLocalizedMessage() );
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken( user.getLogin(), user.getPassword() );
        Authentication result = authenticationManager.authenticate( authentication );
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication( result );
        session = new MockHttpSession();
        session.setAttribute( HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext );

        try
        {
            logger.info( "Create Tomcat server" );
            String jsonString = "{\"applicationName\":\"" + applicationName + "\", \"serverName\":\"" + release + "\"}";
            ResultActions resultats =
                mockMvc.perform( post( "/application" ).session( session ).contentType( MediaType.APPLICATION_JSON ).content( jsonString ) );
            resultats.andExpect( status().isOk() );

        }
        catch ( Exception e )
        {
            logger.error( e.getMessage() );
        }

    }

    @Test( timeout = 2400000 )
    public void test010_DeploySimpleApplicationTest()
        throws Exception
    {
        logger.info( "Deploy an helloworld application" );
        ResultActions resultats =
            mockMvc.perform( MockMvcRequestBuilders.fileUpload( "/application/" + applicationName + "/deploy" ).file( downloadAndPrepareFileToDeploy( "helloworld.war",
                                                                                                                                                      "https://github.com/Treeptik/CloudUnit/releases/download/0.9/helloworld.war" ) ).session( session ).contentType( MediaType.MULTIPART_FORM_DATA ) ).andDo( print() );
        resultats.andExpect( status().is2xxSuccessful() );
        String urlToCall = "http://" + applicationName.toLowerCase() + "-johndoe-admin.cloudunit.dev";
        Assert.assertTrue( getUrlContentPage( urlToCall ).contains( "CloudUnit PaaS" ) );
    }

    @Test( timeout = 2400000 )
    public void test020_DeployMysqlBasedApplicationTest()
        throws Exception
    {
        deployApplicationWithModule( "mysql-5-5", "pizzashop-mysql", "Pizzas" );
    }

    @Test( timeout = 2400000 )
    public void test030_DeployPostGresBasedApplicationTest()
        throws Exception
    {
        deployApplicationWithModule( "postgresql-9-3", "pizzashop-postgres", "Pizzas" );
    }

    @Test( timeout = 2400000 )
    public void test040_DeployMongoBasedApplicationTest()
        throws Exception
    {
        deployApplicationWithModule( "mongo-2-6", "mongo", "Country" );
    }

    private void deployApplicationWithModule(String module, String appName, String keywordInPage)
        throws Exception
    {
        // add the module before deploying war
        String jsonString = "{\"applicationName\":\"" + applicationName + "\", \"imageName\":\"" + module + "\"}";
        ResultActions resultats =
            mockMvc.perform( post( "/module" ).session( session ).contentType( MediaType.APPLICATION_JSON ).content( jsonString ) ).andDo( print() );
        resultats.andExpect( status().isOk() );

        // deploy the war
        logger.info( "Deploy an " + module + " based application" );
        resultats =
            mockMvc.perform(MockMvcRequestBuilders.fileUpload("/application/" + applicationName + "/deploy")
                .file(downloadAndPrepareFileToDeploy(appName + ".war", "https://github.com/Treeptik/CloudUnit/releases/download/0.9/" + appName + ".war"))
                .session(session).contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print());
        // test the application content page
        resultats.andExpect( status().is2xxSuccessful() );
        String urlToCall = "http://" + applicationName.toLowerCase() + "-johndoe-admin.cloudunit.dev";
        String contentPage = getUrlContentPage(urlToCall);
        int counter = 0;
        while (contentPage.contains("Welcome to WildFly") && counter++ < 10) {
            contentPage = getUrlContentPage(urlToCall);
            Thread.sleep(1000);
        }
        Assert.assertTrue(contentPage.contains(keywordInPage));


        // remove the module
        resultats =
            mockMvc.perform( delete( "/module/" + applicationName + "/" + module + "-1" ).session( session ).contentType( MediaType.APPLICATION_JSON ) ).andDo( print() );
        resultats.andExpect( status().isOk() );
    }

    @After
    public void test09_DeleteApplication()
        throws Exception
    {
        logger.info( "Delete application : " + applicationName );
        ResultActions resultats =
            mockMvc.perform( delete( "/application/" + applicationName ).session( session ).contentType( MediaType.APPLICATION_JSON ) );
        resultats.andExpect( status().isOk() );
    }

}
