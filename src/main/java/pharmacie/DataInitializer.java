package pharmacie;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pharmacie.dao.CategorieRepository;
import pharmacie.dao.FournisseurRepository;
import pharmacie.entity.Categorie;
import pharmacie.entity.Fournisseur;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        if (fournisseurRepository.count() > 0) {
            System.out.println("Fournisseurs deja en base, initialisation ignoree.");
            return;
        }

        List<Categorie> categories = categorieRepository.findAll();

        if (categories.isEmpty()) {
            System.out.println("Aucune categorie trouvee.");
            return;
        }

        Fournisseur f1 = new Fournisseur("Grossiste Pharma Plus", "natasamba28+fPharmaplus@gmail.com");
        Fournisseur f2 = new Fournisseur("Medica Distribution", "natasamba28+fMedicaDistribution@gmail.com");
        Fournisseur f3 = new Fournisseur("Express Sante", "natasamba28+fExpressSante@gmail.com");

        for (Categorie cat : categories) {
            f1.getCategories().add(cat);
            f2.getCategories().add(cat);
            if (cat.getCode() != null && cat.getCode() % 2 != 0) {
                f3.getCategories().add(cat);
            }
        }

        fournisseurRepository.save(f1);
        fournisseurRepository.save(f2);
        fournisseurRepository.save(f3);

        System.out.println("Fournisseurs crees et lies aux " + categories.size() + " categories !");
    }
}